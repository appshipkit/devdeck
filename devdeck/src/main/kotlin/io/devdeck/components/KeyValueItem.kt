package io.devdeck.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.devdeck.internal.copyToClipboard

@Composable
public fun KeyValueItem(label: String, value: String, modifier: Modifier = Modifier, copyable: Boolean = false) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .clickable(enabled = copyable) { context.copyToClipboard(label, value) }
            .padding(horizontal = HorizontalPadding, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
