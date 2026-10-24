package io.devdeck.gesture

import kotlin.math.abs

internal class TouchPoint(val id: Long, val x: Float, val y: Float)

internal class TwoFingerSwipeTracker(private val minDistancePx: Float) {

    private var start: Map<Long, TouchPoint>? = null
    private var consumed = false

    fun update(pointers: List<TouchPoint>, towardsLeft: Boolean): Boolean {
        when {
            pointers.isEmpty() -> {
                start = null
                consumed = false
                return false
            }
            pointers.size != FINGERS -> {
                start = null
                if (pointers.size > FINGERS) consumed = true
                return false
            }
        }
        if (consumed) return false
        val origin = start ?: pointers.associateBy { it.id }.also { start = it }
        if (pointers.any { it.id !in origin }) {
            start = pointers.associateBy { it.id }
            return false
        }
        val swiped = pointers.all { pointer ->
            val from = origin.getValue(pointer.id)
            val dx = pointer.x - from.x
            val dy = pointer.y - from.y
            val directional = if (towardsLeft) -dx else dx
            directional >= minDistancePx && directional >= abs(dy) * HORIZONTAL_RATIO
        }
        if (swiped) consumed = true
        return swiped
    }

    private companion object {
        const val FINGERS = 2
        const val HORIZONTAL_RATIO = 1.5f
    }
}
