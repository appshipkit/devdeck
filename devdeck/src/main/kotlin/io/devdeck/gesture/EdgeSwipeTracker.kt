package io.devdeck.gesture

import kotlin.math.abs

/**
 * Detects a one-finger swipe that starts within [edgeWidthPx] of the drawer's edge and moves towards the middle.
 * A second finger cancels it, so a two-finger swipe never also counts as an edge swipe.
 */
internal class EdgeSwipeTracker(private val minDistancePx: Float, private val edgeWidthPx: Float) {

    private var start: TouchPoint? = null
    private var consumed = false

    /** [towardsLeft] means the drawer is on the right, so the swipe starts at the right edge of [widthPx]. */
    fun update(pointers: List<TouchPoint>, towardsLeft: Boolean, widthPx: Float): Boolean {
        if (pointers.isEmpty()) {
            start = null
            consumed = false
            return false
        }
        if (pointers.size > 1) consumed = true
        if (consumed) return false
        val pointer = pointers.single()
        val origin = start ?: pointer.also {
            val fromEdge = if (towardsLeft) widthPx - it.x else it.x
            // A touch that doesn't begin at the edge can't become an edge swipe until every finger lifts.
            if (fromEdge > edgeWidthPx) consumed = true
            start = it
        }
        if (consumed || pointer.id != origin.id) return false
        val dx = pointer.x - origin.x
        val directional = if (towardsLeft) -dx else dx
        val swiped = directional >= minDistancePx && directional >= abs(pointer.y - origin.y) * HORIZONTAL_RATIO
        if (swiped) consumed = true
        return swiped
    }

    private companion object {
        const val HORIZONTAL_RATIO = 1.5f
    }
}
