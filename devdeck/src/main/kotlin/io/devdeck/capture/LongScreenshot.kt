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

        val width = scrollBounds.width()
        val tileHeight = scrollBounds.height()
        val reader = ImageReader.newInstance(width, tileHeight, PixelFormat.RGBA_8888, 2)
        val session = ScrollCaptureSession(reader.surface, scrollBounds, position)
        val tiles = mutableListOf<Pair<Int, Bitmap>>()
        try {
            step<Unit> { done -> callback.onScrollCaptureStart(session, signal) { done(Unit) } } ?: return null
            // The visible area first, then upwards to the top, then downwards to the end, within a height limit.
            captureTile(callback, session, signal, reader, Rect(scrollBounds))?.let { tiles += it }
            var top = scrollBounds.top
            while (tiles.span() < tileHeight * MAX_SCREENS) {
                val request = Rect(scrollBounds.left, top - tileHeight, scrollBounds.right, top)
                val tile = captureTile(callback, session, signal, reader, request) ?: break
                tiles += tile
                if (tile.second.height < tileHeight) break
                top -= tileHeight
            }
            var bottom = scrollBounds.bottom
            while (tiles.span() < tileHeight * MAX_SCREENS) {
                val request = Rect(scrollBounds.left, bottom, scrollBounds.right, bottom + tileHeight)
                val tile = captureTile(callback, session, signal, reader, request) ?: break
                tiles += tile
                if (tile.second.height < tileHeight) break
                bottom += tileHeight
            }
        } finally {
            // Lets the app scroll back to where it was.
            step<Unit> { done -> callback.onScrollCaptureEnd { done(Unit) } }
            reader.close()
        }
        if (tiles.isEmpty()) return null
        return stitch(
            window,
            tiles,
            top = position.y + scrollBounds.top,
            bottom = position.y + scrollBounds.bottom,
            left =
                position.x + scrollBounds.left,
        )
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
    ): Pair<Int, Bitmap>? {
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
            captured.top to Bitmap.createBitmap(full, 0, 0, captured.width().coerceAtMost(it.width), height)
        }
    }

    private fun stitch(window: Bitmap, tiles: List<Pair<Int, Bitmap>>, top: Int, bottom: Int, left: Int): Bitmap {
        val contentTop = tiles.minOf { it.first }
        val contentHeight = tiles.maxOf { it.first + it.second.height } - contentTop
        val above = top.coerceIn(0, window.height)
        val below = (window.height - bottom).coerceAtLeast(0)
        val result = createBitmap(window.width, above + contentHeight + below)
        val canvas = Canvas(result)
        // Toolbar and anything else above the list, then the stitched list, then what's below it (e.g. a bottom bar).
        // Side margins next to a narrower list take the screen's background colour.
        canvas.drawColor(window[0, top.coerceIn(0, window.height - 1)])
        canvas.drawBitmap(window, Rect(0, 0, window.width, above), Rect(0, 0, window.width, above), null)
        tiles.forEach { (y, bitmap) -> canvas.drawBitmap(bitmap, left.toFloat(), (above + y - contentTop).toFloat(), null) }
        if (below > 0) {
            canvas.drawBitmap(
                window,
                Rect(0, bottom, window.width, window.height),
                Rect(0, above + contentHeight, window.width, above + contentHeight + below),
                null,
            )
        }
        return result
    }

    private fun List<Pair<Int, Bitmap>>.span(): Int = if (isEmpty()) 0 else maxOf { it.first + it.second.height } - minOf { it.first }

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
    private const val MAX_SCREENS = 12
    private const val STEP_TIMEOUT_MILLIS = 5_000L
}
