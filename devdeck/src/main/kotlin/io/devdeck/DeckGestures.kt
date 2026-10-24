package io.devdeck

import androidx.compose.runtime.Immutable

@Immutable
public data class DeckGestures(
    val twoFingerSwipe: Boolean = true,
    val keyboardShortcut: DeckShortcut? = DeckShortcut(),
    val swipeToClose: Boolean = true,
) {
    public companion object {
        public val Disabled: DeckGestures = DeckGestures(twoFingerSwipe = false, keyboardShortcut = null)
    }
}
