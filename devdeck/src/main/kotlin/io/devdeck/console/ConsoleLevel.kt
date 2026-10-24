package io.devdeck.console

import android.util.Log
import androidx.compose.ui.graphics.Color

public enum class ConsoleLevel(public val letter: Char, public val label: String, internal val color: Color) {
    Verbose('V', "Verbose", Color(0xFF9E9E9E)),
    Debug('D', "Debug", Color(0xFF1E88E5)),
    Info('I', "Info", Color(0xFF2E7D32)),
    Warn('W', "Warn", Color(0xFFF57C00)),
    Error('E', "Error", Color(0xFFD32F2F)),
    Assert('A', "Assert", Color(0xFF8E24AA)),
    ;

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
