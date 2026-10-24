package io.devdeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun Pill(text: String, color: Color, filled: Boolean = true) {
    val shape = CircleShape
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        fontWeight = FontWeight.Bold,
        color = if (filled) Color.White else color,
        modifier = Modifier
            .then(if (filled) Modifier.background(color, shape) else Modifier.border(1.dp, color, shape))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
