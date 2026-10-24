package io.devdeck.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoFingerSwipeTrackerTest {

    private val tracker = TwoFingerSwipeTracker(minDistancePx = 100f)

    private fun fingers(vararg points: Pair<Float, Float>) = points.mapIndexed { index, (x, y) -> TouchPoint(index.toLong(), x, y) }

    @Test
    fun `two fingers moving left far enough trigger once`() {
        assertFalse(tracker.update(fingers(500f to 500f, 500f to 600f), towardsLeft = true))
        assertTrue(tracker.update(fingers(380f to 505f, 390f to 610f), towardsLeft = true))
        assertFalse(tracker.update(fingers(300f to 505f, 300f to 610f), towardsLeft = true))
    }

    @Test
    fun `short movement does not trigger`() {
        tracker.update(fingers(500f to 500f, 500f to 600f), towardsLeft = true)
        assertFalse(tracker.update(fingers(450f to 500f, 450f to 600f), towardsLeft = true))
    }

    @Test
    fun `wrong direction does not trigger`() {
        tracker.update(fingers(500f to 500f, 500f to 600f), towardsLeft = true)
        assertFalse(tracker.update(fingers(650f to 500f, 650f to 600f), towardsLeft = true))
    }

    @Test
    fun `right swipe triggers for a left side drawer`() {
        tracker.update(fingers(100f to 500f, 100f to 600f), towardsLeft = false)
        assertTrue(tracker.update(fingers(250f to 500f, 250f to 600f), towardsLeft = false))
    }

    @Test
    fun `pinch is ignored`() {
        tracker.update(fingers(500f to 500f, 600f to 500f), towardsLeft = true)
        assertFalse(tracker.update(fingers(350f to 500f, 750f to 500f), towardsLeft = true))
    }

    @Test
    fun `mostly vertical movement is ignored`() {
        tracker.update(fingers(500f to 500f, 500f to 600f), towardsLeft = true)
        assertFalse(tracker.update(fingers(390f to 300f, 390f to 400f), towardsLeft = true))
    }

    @Test
    fun `single finger is ignored`() {
        tracker.update(fingers(500f to 500f), towardsLeft = true)
        assertFalse(tracker.update(fingers(100f to 500f), towardsLeft = true))
    }

    @Test
    fun `three fingers block the gesture until all are lifted`() {
        tracker.update(fingers(500f to 500f, 500f to 600f, 500f to 700f), towardsLeft = true)
        tracker.update(fingers(500f to 500f, 500f to 600f), towardsLeft = true)
        assertFalse(tracker.update(fingers(300f to 500f, 300f to 600f), towardsLeft = true))

        assertEquals(false, tracker.update(emptyList(), towardsLeft = true))
        tracker.update(fingers(500f to 500f, 500f to 600f), towardsLeft = true)
        assertTrue(tracker.update(fingers(300f to 500f, 300f to 600f), towardsLeft = true))
    }
}
