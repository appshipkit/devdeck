package io.devdeck.capture

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.view.ScrollCaptureCallback
import android.view.ScrollCaptureSession
import android.view.ScrollCaptureTarget
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Captures the screen's main scrolling content as one tall image, the way Android's own "Capture more" does:
 * it asks the app's scrollable view (ScrollView, RecyclerView, ListView or Compose) for tiles through the
 * scroll capture API and stitches them between the parts of the window above and below the list.
 * It starts at what's on screen and stops at the end of the content, after a few screens, after a time limit,
 * or when scrolling brings nothing new, so endless feeds can't run it out of memory.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal object LongScreenshot {

    /** Returns null when the screen has nothing to scroll. Call on the main thread. */
    suspend fun capture(activity: Activity, window: Bitmap): Bitmap? {
        val decorView = activity.window?.decorView ?: return null
        val target = findTarget(decorView) ?: return null
        val callback = target.callback
        val signal = CancellationSignal()

        val scrollBounds = step<Rect> { done -> callback.onScrollCaptureSearch(signal) { done(it) } }
        if (scrollBounds == null || scrollBounds.isEmpty) return null
        target.updatePositionInWindow()
        val position = target.positionInWindow

        val tileHeight = scrollBounds.height()
        // Window parts above and below the list (toolbar, bottom bar) frame the stitched content.
        val above = (position.y + scrollBounds.top).coerceIn(0, window.height)
        val listBottom = (position.y + scrollBounds.bottom).coerceIn(above, window.height)
        val below = window.height - listBottom
        val left = position.x + scrollBounds.left
        // Bounded for endless feeds: a few screens and a pixel budget, so the image stays small enough to keep in memory.
        val maxContent = minOf(tileHeight * MAX_SCREENS, MAX_PIXELS / window.width).coerceAtLeast(tileHeight)

        // Each tile is drawn straight into the final image, so only one or two tiles are held at a time.
        val result = createBitmap(window.width, above + maxContent + below)
        val canvas = Canvas(result)
        // Side margins next to a narrower list take the screen's background colour.
        canvas.drawColor(window[0, above.coerceAtMost(window.height - 1)])
        canvas.drawBitmap(window, Rect(0, 0, window.width, above), Rect(0, 0, window.width, above), null)

        val reader = ImageReader.newInstance(scrollBounds.width(), tileHeight, PixelFormat.RGBA_8888, 2)
        val session = ScrollCaptureSession(reader.surface, scrollBounds, position)
        var contentHeight = 0
        try {
            step<Unit> { done -> callback.onScrollCaptureStart(session, signal) { done(Unit) } } ?: return null
            // From what's on screen downwards only, so it doesn't load everything above a deep scroll position.
            withTimeoutOrNull(TOTAL_TIME_MILLIS) {
                // Requests are relative to the scroll bounds: 0 is the top of what's on screen now.
                var requestTop = 0
                var previous: Bitmap? = null
                while (contentHeight < maxContent) {
                    val request = Rect(0, requestTop, scrollBounds.width(), requestTop + tileHeight)
                    val tile = captureTile(callback, session, signal, reader, request) ?: break
                    // Nothing new arrived (e.g. a feed still loading its next page): stop instead of repeating it.
                    if (previous != null && tile.sameAs(previous)) {
                        tile.recycle()
                        break
                    }
                    val height = minOf(tile.height, maxContent - contentHeight)
                    val y = above + contentHeight
                    canvas.drawBitmap(tile, Rect(0, 0, tile.width, height), Rect(left, y, left + tile.width, y + height), null)
                    contentHeight += height
                    previous?.recycle()
                    previous = tile
                    if (tile.height < tileHeight) break
                    requestTop += tileHeight
                }
                previous?.recycle()
            }
        } finally {
            // Lets the app scroll back to where it was.
            step<Unit> { done -> callback.onScrollCaptureEnd { done(Unit) } }
            reader.close()
        }
        if (contentHeight == 0) {
            result.recycle()
            return null
        }
        if (below > 0) {
            val y = above + contentHeight
            canvas.drawBitmap(window, Rect(0, listBottom, window.width, window.height), Rect(0, y, window.width, y + below), null)
        }
        // Trim to what was captured without copying: the rows on top keep their place in the same allocation.
        result.reconfigure(window.width, above + contentHeight + below, Bitmap.Config.ARGB_8888)
        return result
    }

    private fun findTarget(decorView: View): ScrollCaptureTarget? {
        val targets = mutableListOf<ScrollCaptureTarget>()
        decorView.dispatchScrollCaptureSearch(Rect(0, 0, decorView.width, decorView.height), Point(0, 0)) { targets += it }
        return targets
            .filterNot { it.containingView.isInsideDeckOverlay() || it.hint == View.SCROLL_CAPTURE_HINT_EXCLUDE }
            .maxWithOrNull(
                compareBy<ScrollCaptureTarget> { it.hint == View.SCROLL_CAPTURE_HINT_INCLUDE }
                    .thenBy { it.localVisibleRect.width() * it.localVisibleRect.height() },
            )
    }

    private suspend fun captureTile(
        callback: ScrollCaptureCallback,
        session: ScrollCaptureSession,
        signal: CancellationSignal,
        reader: ImageReader,
        request: Rect,
    ): Bitmap? {
        // Drop any stale frame so the next one is this request's.
        reader.acquireLatestImage()?.close()
        val captured = step<Rect> { done -> callback.onScrollCaptureImageRequest(session, signal, request) { done(it) } }
        if (captured == null || captured.isEmpty) return null
        val image = step<Image> { done ->
            reader.acquireLatestImage()?.let(done)
                ?: reader.setOnImageAvailableListener({ it.acquireLatestImage()?.let(done) }, Handler(Looper.getMainLooper()))
        } ?: return null
        reader.setOnImageAvailableListener(null, null)
        return image.use {
            val plane = it.planes[0]
            val full = createBitmap(plane.rowStride / plane.pixelStride, it.height)
            full.copyPixelsFromBuffer(plane.buffer)
            // The captured area is drawn at the top-left of the buffer, even when it's smaller than the request.
            val height = captured.height().coerceAtMost(it.height)
            Bitmap.createBitmap(full, 0, 0, captured.width().coerceAtMost(it.width), height).also { full.recycle() }
        }
    }

    private fun View.isInsideDeckOverlay(): Boolean {
        var view: View? = this
        while (view != null) {
            if (view.tag == DECK_OVERLAY_TAG) return true
            view = view.parent as? View
        }
        return false
    }

    /** Runs a callback-style step on the main thread and waits for its single result, giving up after a timeout. */
    private suspend fun <T> step(block: (done: (T) -> Unit) -> Unit): T? = withTimeoutOrNull(STEP_TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
            block { value -> if (continuation.isActive) continuation.resume(value) }
        }
    }

    private const val DECK_OVERLAY_TAG = "io.devdeck.overlay"
    private const val MAX_SCREENS = 5
    private const val MAX_PIXELS = 15_000_000
    private const val TOTAL_TIME_MILLIS = 8_000L
    private const val STEP_TIMEOUT_MILLIS = 5_000L
}
