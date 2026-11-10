package io.devdeck.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Watches touches without consuming them and calls [onSwipe] for an enabled opening swipe. */
internal fun Modifier.deckSwipes(twoFinger: Boolean, oneFinger: Boolean, towardsLeft: Boolean, onSwipe: () -> Unit): Modifier =
    if (!twoFinger && !oneFinger) {
        this
    } else {
        pointerInput(twoFinger, oneFinger, towardsLeft, onSwipe) {
            val twoFingerTracker = TwoFingerSwipeTracker(SWIPE_DISTANCE.toPx())
            val edgeTracker = EdgeSwipeTracker(SWIPE_DISTANCE.toPx(), EDGE_WIDTH.toPx())
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointers = event.changes
                        .filter { it.pressed }
                        .map { TouchPoint(it.id.value, it.position.x, it.position.y) }
                    val twoFingerSwipe = twoFinger && twoFingerTracker.update(pointers, towardsLeft)
                    val edgeSwipe = oneFinger && edgeTracker.update(pointers, towardsLeft, size.width.toFloat())
                    if (twoFingerSwipe || edgeSwipe) onSwipe()
                }
            }
        }
    }

internal val SWIPE_DISTANCE = 72.dp

/** How far from the drawer's edge a one-finger swipe may start. */
internal val EDGE_WIDTH = 24.dp

/** Height of the strip on the drawer's edge that Android's back gesture leaves to the app (the system caps it at 200dp). */
internal val EDGE_EXCLUSION_HEIGHT = 200.dp
