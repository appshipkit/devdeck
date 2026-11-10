package io.devdeck

import androidx.compose.runtime.Immutable

/**
 * How the drawer opens and closes.
 *
 * - [twoFingerSwipe]: a two-finger swipe towards the drawer anywhere on screen.
 * - [oneFingerSwipe]: a one-finger swipe in from the drawer's edge. Off by default because it competes with the
 *   app's own edge gestures; when on, a 200dp strip in the middle of that edge is excluded from Android's back
 *   gesture so the swipe reaches the app.
 * - [keyboardShortcut]: toggles the drawer from a hardware keyboard or the emulator; null turns it off.
 * - [swipeToClose]: dragging the open drawer closes it.
 */
@Immutable
public data class DeckGestures(
    val twoFingerSwipe: Boolean = true,
    val keyboardShortcut: DeckShortcut? = DeckShortcut(),
    val swipeToClose: Boolean = true,
    val oneFingerSwipe: Boolean = false,
) {

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        twoFingerSwipe: Boolean = true,
        keyboardShortcut: DeckShortcut? = DeckShortcut(),
        swipeToClose: Boolean = true,
    ) : this(twoFingerSwipe, keyboardShortcut, swipeToClose, oneFingerSwipe = false)

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        twoFingerSwipe: Boolean = this.twoFingerSwipe,
        keyboardShortcut: DeckShortcut? = this.keyboardShortcut,
        swipeToClose: Boolean = this.swipeToClose,
    ): DeckGestures = copy(
        twoFingerSwipe = twoFingerSwipe,
        keyboardShortcut = keyboardShortcut,
        swipeToClose = swipeToClose,
        oneFingerSwipe = oneFingerSwipe,
    )

    internal val anySwipe: Boolean get() = twoFingerSwipe || oneFingerSwipe

    public companion object {
        public val Disabled: DeckGestures = DeckGestures(twoFingerSwipe = false, keyboardShortcut = null, oneFingerSwipe = false)
    }
}
