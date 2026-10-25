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
import android.os.SystemClock
import android.view.ScrollCaptureCallback
import android.view.ScrollCaptureSession
import android.view.ScrollCaptureTarget
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import io.devdeck.DECK_OVERLAY_TAG
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Captures the screen's main scrolling content as one tall image, the way Android's own "Capture more" does:
 * it asks the app's scrollable view (ScrollView, RecyclerView, ListView or Compose) for tiles through the
 * scroll capture API and stitches them between the parts of the window above and below the list.
 * It starts at what's on screen and stops at the end of the content, after a few screens, after a time limit for
 * the whole capture, or when scrolling brings nothing new, so endless feeds can't run it out of memory.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal object LongScreenshot {

    sealed interface Result {
        class Captured(val bitmap: Bitmap) : Result

        data object NothingToScroll : Result

        data object Failed : Result
    }

    /** Captures from what's on screen downwards, within [TOTAL_TIME_MILLIS] for the whole capture. Call on the main thread. */
    suspend fun capture(activity: Activity, window: Bitmap): Result {
        val decorView = activity.window?.decorView ?: return Result.Failed
        val target = findTarget(decorView) ?: return Result.NothingToScroll
        val callback = target.callback
        val deadline = SystemClock.uptimeMillis() + TOTAL_TIME_MILLIS

        val scrollBounds = step<Rect>(deadline) { signal, done -> callback.onScrollCaptureSearch(signal) { done(it) } }
            ?: return Result.Failed
        if (scrollBounds.isEmpty) return Result.NothingToScroll
        target.updatePositionInWindow()
        val position = target.positionInWindow

        val tileWidth = scrollBounds.width()
        val tileHeight = scrollBounds.height()
        // Window parts above and below the list (toolbar, bottom bar) frame the stitched content.
        val above = (position.y + scrollBounds.top).coerceIn(0, window.height)
        val listBottom = (position.y + scrollBounds.bottom).coerceIn(above, window.height)
        val below = window.height - listBottom
        val left = position.x + scrollBounds.left
        // Bounded for endless feeds: a few screens and a pixel budget, so the image stays small enough to keep in memory.
        val maxContent = minOf(tileHeight * MAX_SCREENS, MAX_PIXELS / window.width).coerceAtLeast(tileHeight)

        // Each tile is drawn straight into the final image, so only one or two tiles are held at a time.
        val result = try {
            createBitmap(window.width, above + maxContent + below)
        } catch (_: OutOfMemoryError) {
            return Result.Failed
        }
        val canvas = Canvas(result)
        // Side margins next to a narrower list take the screen's background colour.
        canvas.drawColor(window[0, above.coerceAtMost(window.height - 1)])
        canvas.drawBitmap(window, Rect(0, 0, window.width, above), Rect(0, 0, window.width, above), null)

        val reader = ImageReader.newInstance(tileWidth, tileHeight, PixelFormat.RGBA_8888, 2)
        val session = ScrollCaptureSession(reader.surface, scrollBounds, position)
        var contentBottom = 0
        try {
            step<Unit>(deadline) { signal, done -> callback.onScrollCaptureStart(session, signal) { done(Unit) } }
                ?: return Result.Failed.also { result.recycle() }
            // Requests are relative to the scroll bounds: 0 is the top of what's on screen now. Only downwards,
            // so a deep scroll position doesn't load everything above it.
            var requestTop = 0
            var previous: Bitmap? = null
            while (requestTop < maxContent) {
                val request = Rect(0, requestTop, tileWidth, requestTop + tileHeight)
                val (captured, tile) = captureTile(callback, session, reader, request, deadline) ?: break
                // Nothing new arrived (e.g. a feed still loading its next page): stop instead of repeating it.
                if (previous != null && tile.sameAs(previous)) {
                    tile.recycle()
                    break
                }
                val top = captured.top.coerceAtLeast(0)
                val height = minOf(tile.height, maxContent - top)
                if (height > 0) {
                    val y = above + top
                    canvas.drawBitmap(tile, Rect(0, 0, tile.width, height), Rect(left, y, left + tile.width, y + height), null)
                    contentBottom = maxOf(contentBottom, top + height)
                }
                previous?.recycle()
                previous = tile
                // A short tile means the content ended.
                if (captured.bottom < request.bottom || height <= 0) break
                requestTop = captured.bottom
            }
            previous?.recycle()
        } finally {
            // Always end the session, even after a timeout, so the app scrolls back to where it was.
            withContext(NonCancellable) {
                step<Unit>(SystemClock.uptimeMillis() + END_TIMEOUT_MILLIS) { _, done -> callback.onScrollCaptureEnd { done(Unit) } }
            }
            reader.close()
        }
        if (contentBottom == 0) {
            result.recycle()
            return Result.Failed
        }
        if (below > 0) {
            val y = above + contentBottom
            canvas.drawBitmap(window, Rect(0, listBottom, window.width, window.height), Rect(0, y, window.width, y + below), null)
        }
        // Trim to what was captured without copying: the rows on top keep their place in the same allocation.
        result.reconfigure(window.width, above + contentBottom + below, Bitmap.Config.ARGB_8888)
        return Result.Captured(result)
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
        reader: ImageReader,
        request: Rect,
        deadline: Long,
    ): Pair<Rect, Bitmap>? {
        // Drop any stale frame so the next one is this request's.
        reader.acquireLatestImage()?.close()
        val captured = step<Rect>(deadline) { signal, done -> callback.onScrollCaptureImageRequest(session, signal, request) { done(it) } }
        if (captured == null || captured.isEmpty) return null
        val image = try {
            step<Image>(deadline) { _, done ->
                // Listen first, then check, so a frame that lands in between isn't missed; extra frames are closed.
                reader.setOnImageAvailableListener({
                    it.acquireLatestImage()?.let { image -> if (!done(image)) image.close() }
                }, mainHandler)
                reader.acquireLatestImage()?.let { image -> if (!done(image)) image.close() }
            }
        } finally {
            reader.setOnImageAvailableListener(null, null)
        } ?: return null
        return image.use { captured to it.toBitmap(captured.width(), captured.height()) }
    }

    // The captured area is drawn at the top-left of the buffer. Rows are copied one by one because the row stride
    // can be wider than the image and some devices don't pad the last row.
    private fun Image.toBitmap(capturedWidth: Int, capturedHeight: Int): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val width = capturedWidth.coerceAtMost(this.width)
        val height = capturedHeight.coerceAtMost(this.height)
        val rowBytes = width * plane.pixelStride
        val packed = ByteBuffer.allocateDirect(rowBytes * height)
        val row = ByteArray(rowBytes)
        for (y in 0 until height) {
            buffer.position(y * plane.rowStride)
            buffer.get(row, 0, rowBytes)
            packed.put(row)
        }
        packed.rewind()
        return createBitmap(width, height).apply { copyPixelsFromBuffer(packed) }
    }

    private fun View.isInsideDeckOverlay(): Boolean {
        var view: View? = this
        while (view != null) {
            if (view.tag == DECK_OVERLAY_TAG) return true
            view = view.parent as? View
        }
        return false
    }

    /**
     * Runs one callback-style step on the main thread and waits for its result until [deadline]. On timeout the
     * step's CancellationSignal is cancelled, as the system does, so the app stops scrolling for it.
     * `done` returns false when the step already finished, so late results can be cleaned up.
     */
    private suspend fun <T> step(deadline: Long, block: (signal: CancellationSignal, done: (T) -> Boolean) -> Unit): T? {
        val remaining = (deadline - SystemClock.uptimeMillis()).coerceAtMost(STEP_TIMEOUT_MILLIS)
        if (remaining <= 0) return null
        val signal = CancellationSignal()
        return withTimeoutOrNull(remaining) {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { signal.cancel() }
                block(signal) { value ->
                    continuation.isActive.also { active -> if (active) continuation.resume(value) }
                }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private const val MAX_SCREENS = 5
    private const val MAX_PIXELS = 15_000_000
    private const val TOTAL_TIME_MILLIS = 8_000L
    private const val STEP_TIMEOUT_MILLIS = 5_000L
    private const val END_TIMEOUT_MILLIS = 2_000L
}
