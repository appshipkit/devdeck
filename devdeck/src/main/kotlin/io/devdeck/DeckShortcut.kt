package io.devdeck

import android.view.KeyEvent
import androidx.compose.runtime.Immutable

@Immutable
public data class DeckShortcut(
    val keyCode: Int = KeyEvent.KEYCODE_D,
    val ctrl: Boolean = true,
    val shift: Boolean = true,
    val alt: Boolean = false,
) {
    internal fun matches(event: KeyEvent): Boolean = event.keyCode == keyCode &&
        event.isCtrlPressed == ctrl &&
        event.isShiftPressed == shift &&
        event.isAltPressed == alt

    override fun toString(): String = buildList {
        if (ctrl) add("Ctrl")
        if (shift) add("Shift")
        if (alt) add("Alt")
        add(KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_"))
    }.joinToString(" + ")
}

internal fun DeckShortcut?.handle(event: KeyEvent, state: DeckState): Boolean {
    if (this == null || !matches(event)) return false
    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) state.toggle()
    return true
}
