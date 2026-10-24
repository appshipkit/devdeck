package io.devdeck.logs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
public fun LogList(modifier: Modifier = Modifier, tag: String? = null, maxItems: Int = 100) {
    val allEntries by DeckLogger.entries.collectAsState()
    val entries = remember(allEntries, tag, maxItems) {
        allEntries.asSequence().filter { tag == null || it.tag == tag }.take(maxItems).toList()
    }
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (entries.size == 1) "1 entry" else "${entries.size} entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { DeckLogger.clear(tag) }, enabled = entries.isNotEmpty()) {
                Icon(
                    DeckIcons.Delete,
                    contentDescription = "Clear logs",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = if (entries.isEmpty()) 0.38f else 1f),
                )
            }
        }
        if (entries.isEmpty()) TextItem("No logs yet")
        entries.forEach { entry -> LogRow(entry, formatter.format(Date(entry.timestampMillis)), showTag = tag == null) }
    }
}

@Composable
private fun LogRow(entry: LogEntry, time: String, showTag: Boolean) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.payload != null) { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row {
            Text(
                text = if (showTag) "$time · ${entry.tag}" else time,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(entry.message, style = MaterialTheme.typography.bodyLarge)
        if (expanded && entry.payload != null) {
            Text(
                entry.payload,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
