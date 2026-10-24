package io.devdeck.tools

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public object UiTools {

    public var isKeylineOverlayEnabled: Boolean by mutableStateOf(false)

    public var keylineGridSize: Dp by mutableStateOf(8.dp)

    public var keylines: List<Dp> by mutableStateOf(listOf(16.dp, 72.dp))

    public var isAnimationSlowedDown: Boolean by mutableStateOf(false)
        private set

    private var originalDurationScale: Float? = null

    public fun setAnimationsSlowedDown(slowedDown: Boolean, slowScale: Float = 4f): Boolean {
        if (originalDurationScale == null) {
            originalDurationScale = currentDurationScale()
        }
        val scale = if (slowedDown) slowScale else originalDurationScale ?: 1f
        val applied = runCatching {
            ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType).invoke(null, scale)
        }.isSuccess
        if (applied) isAnimationSlowedDown = slowedDown
        return applied
    }

    private fun currentDurationScale(): Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ValueAnimator.getDurationScale()
    } else {
        runCatching { ValueAnimator::class.java.getMethod("getDurationScale").invoke(null) as Float }.getOrDefault(1f)
    }
}
