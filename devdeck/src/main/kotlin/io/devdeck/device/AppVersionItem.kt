package io.devdeck.device

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.devdeck.components.RowPadding
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.copyToClipboard

@Composable
public fun AppVersionItem(modifier: Modifier = Modifier, text: String? = null) {
    val context = LocalContext.current
    val label = text ?: remember(context) {
        val app = appVersionInfo(context)
        val name = context.applicationInfo.loadLabel(context.packageManager)
        if (app == null) name.toString() else "$name v${app.versionName} (${app.versionCode})"
    }
    Row(modifier.fillMaxWidth().then(RowPadding), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { context.copyToClipboard("Version", label) }) {
            Icon(
                DeckIcons.ContentCopy,
                contentDescription = "Copy version",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
