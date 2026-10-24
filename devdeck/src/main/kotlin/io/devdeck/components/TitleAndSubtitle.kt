package io.devdeck.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun TitleAndSubtitle(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (enabled) 1f else 0.38f
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
