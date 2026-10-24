package io.devdeck.performance

import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Immutable
internal data class FrameStats(val fps: Int, val jankyFrames: Int)

@Composable
internal fun rememberFrameStats(): State<FrameStats> {
    val stats = remember { mutableStateOf(FrameStats(fps = 0, jankyFrames = 0)) }
    DisposableEffect(Unit) {
        val choreographer = Choreographer.getInstance()
        var windowStart = 0L
        var lastFrame = 0L
        var frames = 0
        var janky = 0
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (windowStart == 0L) windowStart = frameTimeNanos
                if (lastFrame != 0L && frameTimeNanos - lastFrame > JANK_THRESHOLD_NANOS) janky++
                lastFrame = frameTimeNanos
                frames++
                val elapsed = frameTimeNanos - windowStart
                if (elapsed >= WINDOW_NANOS) {
                    stats.value = FrameStats(fps = (frames * 1_000_000_000L / elapsed).toInt(), jankyFrames = janky)
                    windowStart = frameTimeNanos
                    frames = 0
                    janky = 0
                }
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
        onDispose { choreographer.removeFrameCallback(callback) }
    }
    return stats
}

private const val WINDOW_NANOS = 500_000_000L
private const val JANK_THRESHOLD_NANOS = 32_000_000L
