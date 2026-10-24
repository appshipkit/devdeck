package io.devdeck.internal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.devdeck.LocalDeckColors

/** A category color with a light-theme shade that reads on white and a dark-theme shade that reads on dark surfaces. */
@Immutable
internal class Tone(val light: Color, val dark: Color) {
    fun pick(isDark: Boolean): Color = if (isDark) dark else light
}

internal object DeckPalette {
    val Blue = Tone(Color(0xFF1565C0), Color(0xFF64B5F6))
    val Purple = Tone(Color(0xFF8E24AA), Color(0xFFCE93D8))
    val DeepPurple = Tone(Color(0xFF5E35B1), Color(0xFFB39DDB))
    val Teal = Tone(Color(0xFF00796B), Color(0xFF80CBC4))
    val Green = Tone(Color(0xFF2E7D32), Color(0xFF81C784))
    val Orange = Tone(Color(0xFFB45309), Color(0xFFFFB74D))
    val DeepOrange = Tone(Color(0xFFBF360C), Color(0xFFFFAB91))
    val Red = Tone(Color(0xFFC62828), Color(0xFFEF9A9A))
    val Grey = Tone(Color(0xFF616161), Color(0xFFBDBDBD))
}

@Composable
@ReadOnlyComposable
internal fun isDeckDark(): Boolean = LocalDeckColors.current?.isDark ?: isSystemInDarkTheme()

/** White or near-black, whichever contrasts more with [fill]. */
internal fun contentColorOn(fill: Color): Color = if (fill.luminance() > LUMINANCE_SPLIT) DarkContent else Color.White

private val DarkContent = Color(0xFF1C1B1F)

// Above this luminance black text has more contrast than white.
private const val LUMINANCE_SPLIT = 0.179f
