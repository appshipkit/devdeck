package io.devdeck.storage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.BulletItem
import io.devdeck.components.InfoGroup
import io.devdeck.components.TextItem
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
public fun StorageUsage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val usage by produceState<List<Pair<String, Long>>?>(null, context) {
        value = withContext(Dispatchers.IO) { StorageFiles.usage(context) }
    }
    Column(modifier.fillMaxWidth()) {
        val items = usage
        if (items == null) {
            TextItem("Calculating…")
        } else {
            InfoGroup {
                items.forEach { (label, bytes) -> BulletItem(label, formatBytes(bytes)) }
                BulletItem("Total", formatBytes(items.sumOf { it.second }))
            }
        }
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
