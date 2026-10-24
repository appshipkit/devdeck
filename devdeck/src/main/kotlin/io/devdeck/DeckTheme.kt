package io.devdeck

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.sp

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
    MaterialTheme(colorScheme = colorScheme, typography = DeckTypography) {
        CompositionLocalProvider(LocalDeckColors provides colors, content = content)
    }
}

// A notch smaller than Material's defaults: the drawer is narrow, and labels and descriptions should fit on one line more often.
private val DeckTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = titleMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
    )
}

private val LightPanel = Color(0xFFF2F3F7)
private val LightWarning = Color(0xFFE65100)
private val DarkWarning = Color(0xFFFFB74D)
