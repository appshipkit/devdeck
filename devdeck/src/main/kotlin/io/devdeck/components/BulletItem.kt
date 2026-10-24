package io.devdeck.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.devdeck.internal.copyToClipboard

@Composable
public fun BulletItem(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: (() -> Unit)? = null) {
    BulletRow(modifier, enabled, onClick) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}

@Composable
public fun BulletItem(label: String, value: String, modifier: Modifier = Modifier, copyable: Boolean = false) {
    val context = LocalContext.current
    BulletRow(modifier, enabled = true, onClick = if (copyable) ({ context.copyToClipboard(label, value) }) else null) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$label: ") }
                append(value)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BulletRow(modifier: Modifier, enabled: Boolean, onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(start = 28.dp, end = HorizontalPadding, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("•", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(40.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}
