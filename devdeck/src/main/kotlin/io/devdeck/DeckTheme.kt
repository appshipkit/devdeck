package io.devdeck

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

@Immutable
internal data class DeckColors(val panel: Color, val card: Color, val warning: Color, val isDark: Boolean)

internal val LocalDeckColors = staticCompositionLocalOf<DeckColors?> { null }

@Composable
internal fun DeckTheme(style: DeckStyle, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colorScheme = if (dark) style.darkColorScheme ?: darkColorScheme() else style.lightColorScheme ?: lightColorScheme()
    val colors = DeckColors(
        panel = when {
            style.containerColor.isSpecified -> style.containerColor
            dark -> colorScheme.surface
            else -> LightPanel
        },
        card = when {
            style.cardColor.isSpecified -> style.cardColor
            dark -> colorScheme.surfaceContainerHigh
            else -> Color.White
        },
        warning = if (dark) DarkWarning else LightWarning,
        isDark = dark,
    )
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalDeckColors provides colors, content = content)
    }
}

private val LightPanel = Color(0xFFF2F3F7)
private val LightWarning = Color(0xFFE65100)
private val DarkWarning = Color(0xFFFFB74D)
