package io.devdeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.devdeck.internal.copyToClipboard

@Composable
public fun BulletItem(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: (() -> Unit)? = null) {
    InfoRow(modifier, enabled, onClick) {
        Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
public fun BulletItem(label: String, value: String, modifier: Modifier = Modifier, copyable: Boolean = false) {
    val context = LocalContext.current
    InfoRow(
        modifier,
        enabled = true,
        onClick = if (copyable) ({ context.copyToClipboard(label, value) }) else null,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(LABEL_WEIGHT, fill = false).padding(end = 16.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(VALUE_WEIGHT, fill = false),
        )
    }
}

@Composable
private fun InfoRow(
    modifier: Modifier,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .heightIn(min = if (onClick != null) 48.dp else 36.dp)
            .padding(horizontal = HorizontalPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

private const val LABEL_WEIGHT = 0.45f
private const val VALUE_WEIGHT = 0.55f
