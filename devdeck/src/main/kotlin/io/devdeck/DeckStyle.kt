package io.devdeck

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class DeckStyle(
    val title: String = "Debug menu",
    val subtitle: String? = null,
    val maxWidth: Dp = 360.dp,
    val widthFraction: Float = 0.85f,
    val scrimColor: Color = Color.Black.copy(alpha = 0.4f),
    val containerColor: Color = Color.Unspecified,
    val cardColor: Color = Color.Unspecified,
    val lightColorScheme: ColorScheme? = null,
    val darkColorScheme: ColorScheme? = null,
    val animationDurationMillis: Int = 250,
    val collapsibleSections: Boolean = true,
)
