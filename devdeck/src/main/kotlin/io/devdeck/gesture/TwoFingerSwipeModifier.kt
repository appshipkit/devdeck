package io.devdeck.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

internal fun Modifier.twoFingerSwipe(enabled: Boolean, towardsLeft: Boolean, onSwipe: () -> Unit): Modifier = if (!enabled) {
    this
} else {
    pointerInput(towardsLeft, onSwipe) {
        val tracker = TwoFingerSwipeTracker(SWIPE_DISTANCE.toPx())
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pointers = event.changes
                    .filter { it.pressed }
                    .map { TouchPoint(it.id.value, it.position.x, it.position.y) }
                if (tracker.update(pointers, towardsLeft)) onSwipe()
            }
        }
    }
}

internal val SWIPE_DISTANCE = 72.dp
