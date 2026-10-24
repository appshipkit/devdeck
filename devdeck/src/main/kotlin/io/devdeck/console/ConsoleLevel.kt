package io.devdeck.console

import android.util.Log
import androidx.compose.ui.graphics.Color
import io.devdeck.internal.DeckPalette
import io.devdeck.internal.Tone

public enum class ConsoleLevel(public val letter: Char, public val label: String, private val tone: Tone) {
    Verbose('V', "Verbose", DeckPalette.Grey),
    Debug('D', "Debug", DeckPalette.Blue),
    Info('I', "Info", DeckPalette.Green),
    Warn('W', "Warn", DeckPalette.Orange),
    Error('E', "Error", DeckPalette.Red),
    Assert('A', "Assert", DeckPalette.Purple),
    ;

    /** Light-theme color, kept for existing callers. */
    internal val color: Color get() = tone.light

    internal fun color(isDark: Boolean): Color = tone.pick(isDark)

    public companion object {
        public fun fromPriority(priority: Int): ConsoleLevel = when (priority) {
            Log.VERBOSE -> Verbose
            Log.DEBUG -> Debug
            Log.INFO -> Info
            Log.WARN -> Warn
            Log.ERROR -> Error
            else -> if (priority >= Log.ASSERT) Assert else Verbose
        }

        public fun fromLetter(letter: Char): ConsoleLevel =
            entries.firstOrNull { it.letter == letter } ?: if (letter == 'F') Assert else Verbose
    }
}
