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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.components.ConfirmDeleteButton
import io.devdeck.components.InfoGroup
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.shareFiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
public fun LogList(modifier: Modifier = Modifier, tag: String? = null, maxItems: Int = 100) {
    val context = LocalContext.current
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { context.shareFiles(emptyList(), text = format(entries), subject = tag?.let { "$it logs" } ?: "Logs") },
                enabled = entries.isNotEmpty(),
            ) { Icon(DeckIcons.Share, contentDescription = "Share logs") }
            ConfirmDeleteButton(contentDescription = "Clear logs", onConfirm = { DeckLogger.clear(tag) }, enabled = entries.isNotEmpty())
        }
        if (entries.isEmpty()) TextItem("No entries yet")
        InfoGroup(dividerThreshold = 2) {
            entries.forEach { entry ->
                key(entry.id) { LogRow(entry, formatter.format(Date(entry.timestampMillis)), showTag = tag == null) }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry, time: String, showTag: Boolean) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (entry.payload != null) {
                    Modifier
                        .clickable(onClickLabel = if (expanded) "Collapse" else "Expand") { expanded = !expanded }
                        .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                } else {
                    Modifier
                },
            )
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

private fun format(entries: List<LogEntry>): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    return entries.asReversed().joinToString("\n") { entry ->
        val payload = entry.payload?.let { "\n    $it" }.orEmpty()
        "${formatter.format(Date(entry.timestampMillis))} [${entry.tag}] ${entry.message}$payload"
    }
}
