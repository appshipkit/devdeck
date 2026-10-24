package io.devdeck.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.devdeck.DeckGestures
import io.devdeck.DeckShortcut
import io.devdeck.DeckSide
import io.devdeck.DeckTrigger

@Stable
class DrawerOptions {
    var enabled by mutableStateOf(true)
    var twoFingerSwipe by mutableStateOf(true)
    var keyboardShortcut by mutableStateOf(true)
    var floatingButton by mutableStateOf(true)
    var side by mutableStateOf(DeckSide.End)

    val gestures: DeckGestures
        get() = DeckGestures(
            twoFingerSwipe = twoFingerSwipe,
            keyboardShortcut = if (keyboardShortcut) DeckShortcut() else null,
        )

    val trigger: DeckTrigger
        get() = if (floatingButton) DeckTrigger.FloatingButton() else DeckTrigger.None
}

@Composable
fun rememberDrawerOptions(): DrawerOptions = remember { DrawerOptions() }
