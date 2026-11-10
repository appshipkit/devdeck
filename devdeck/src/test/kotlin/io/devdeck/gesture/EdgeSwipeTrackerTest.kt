package io.devdeck.gesture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeSwipeTrackerTest {

    private val width = 1000f

    private fun tracker() = EdgeSwipeTracker(minDistancePx = 100f, edgeWidthPx = 30f)

    private fun finger(x: Float, y: Float = 500f, id: Long = 1) = listOf(TouchPoint(id, x, y))

    @Test
    fun `a swipe in from the right edge opens a drawer on the right`() {
        val tracker = tracker()
        assertFalse(tracker.update(finger(990f), towardsLeft = true, widthPx = width))
        assertFalse(tracker.update(finger(940f), towardsLeft = true, widthPx = width))
        assertTrue(tracker.update(finger(880f), towardsLeft = true, widthPx = width))
        assertFalse("fires once per gesture", tracker.update(finger(800f), towardsLeft = true, widthPx = width))
    }

    @Test
    fun `a swipe in from the left edge opens a drawer on the left`() {
        val tracker = tracker()
        tracker.update(finger(10f), towardsLeft = false, widthPx = width)
        assertTrue(tracker.update(finger(150f), towardsLeft = false, widthPx = width))
    }

    @Test
    fun `swipes that start away from the edge are ignored until the finger lifts`() {
        val tracker = tracker()
        tracker.update(finger(700f), towardsLeft = true, widthPx = width)
        assertFalse(tracker.update(finger(400f), towardsLeft = true, widthPx = width))

        tracker.update(emptyList(), towardsLeft = true, widthPx = width)
        tracker.update(finger(995f), towardsLeft = true, widthPx = width)
        assertTrue(tracker.update(finger(850f), towardsLeft = true, widthPx = width))
    }

    @Test
    fun `mostly vertical drags and second fingers don't count`() {
        val vertical = tracker()
        vertical.update(finger(990f, y = 100f), towardsLeft = true, widthPx = width)
        assertFalse(vertical.update(finger(870f, y = 400f), towardsLeft = true, widthPx = width))

        val twoFingers = tracker()
        twoFingers.update(finger(990f), towardsLeft = true, widthPx = width)
        twoFingers.update(finger(980f) + finger(500f, id = 2), towardsLeft = true, widthPx = width)
        assertFalse(twoFingers.update(finger(850f), towardsLeft = true, widthPx = width))
    }

    @Test
    fun `swiping away from the drawer doesn't open it`() {
        val tracker = tracker()
        tracker.update(finger(10f), towardsLeft = true, widthPx = width)
        assertFalse(tracker.update(finger(200f), towardsLeft = true, widthPx = width))
    }
}
