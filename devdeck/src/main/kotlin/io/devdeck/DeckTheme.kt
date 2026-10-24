package io.devdeck

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
internal fun DeckTheme(style: DeckStyle, content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) {
        style.darkColorScheme ?: darkColorScheme()
    } else {
        style.lightColorScheme ?: lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
