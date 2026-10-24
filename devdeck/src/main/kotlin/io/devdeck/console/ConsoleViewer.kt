package io.devdeck.console

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.capture.ScreenCapture
import io.devdeck.components.FilterPill
import io.devdeck.components.FilterPillRow
import io.devdeck.components.SearchField
import io.devdeck.components.SegmentedPills
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.shareFiles
import java.io.File

public enum class ConsoleView(public val label: String) { All("All"), ByTag("By tag") }

@Composable
public fun ConsoleViewer(
    buffer: ConsoleBuffer,
    modifier: Modifier = Modifier,
    title: String = "Logs",
    fixedTag: String? = null,
    initialView: ConsoleView = ConsoleView.All,
    maxRendered: Int = 300,
) {
    val context = LocalContext.current
    val deckState = LocalDeckState.current
    val live by buffer.lines.collectAsState()
    var frozen by remember { mutableStateOf<List<ConsoleLine>?>(null) }
    val source = frozen ?: live
    var query by rememberSaveable { mutableStateOf("") }
    var minLevel by rememberSaveable { mutableStateOf(ConsoleLevel.Verbose) }
    var view by rememberSaveable { mutableStateOf(if (fixedTag != null) ConsoleView.All else initialView) }

    val visible = remember(source, query, minLevel, fixedTag) {
        source.filter { line ->
            line.level >= minLevel &&
                (fixedTag == null || line.tag == fixedTag) &&
                (
                    query.isBlank() ||
                        line.tag.contains(query.trim(), ignoreCase = true) ||
                        line.message.contains(query.trim(), ignoreCase = true)
                    )
        }
    }
    val errors = remember(visible) { visible.count { it.level >= ConsoleLevel.Error } }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${visible.size} lines · $errors errors${if (frozen != null) " · paused" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { frozen = if (frozen == null) live else null }) {
                Icon(
                    if (frozen ==
                        null
                    ) {
                        DeckIcons.Pause
                    } else {
                        DeckIcons.PlayArrow
                    },
                    contentDescription = if (frozen == null) "Pause" else "Resume",
                )
            }
            IconButton(onClick = { shareLines(context, title, visible) }, enabled = visible.isNotEmpty()) {
                Icon(DeckIcons.Share, contentDescription = "Share")
            }
            IconButton(onClick = {
                buffer.clear()
                frozen = null
            }, enabled = source.isNotEmpty()) {
                Icon(
                    DeckIcons.Delete,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = if (source.isEmpty()) 0.38f else 1f),
                )
            }
        }
        if (fixedTag == null) {
            SegmentedPills(
                options = ConsoleView.entries,
                selected = view,
                onSelected = { view = it },
                label = { it.label },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        SearchField(query = query, onQueryChange = { query = it }, placeholder = "Search tag or message")
        FilterPillRow {
            LEVEL_FILTERS.forEach { level ->
                val isAll = level == ConsoleLevel.Verbose
                FilterPill(
                    text = if (isAll) "All" else "${level.label}+",
                    selected = minLevel == level,
                    onClick = { minLevel = level },
                    color = if (isAll) Color.Unspecified else level.color,
                    showDot = !isAll,
                )
            }
        }
        when {
            visible.isEmpty() -> TextItem(if (source.isEmpty()) "Nothing logged yet" else "No lines match")
            view == ConsoleView.ByTag -> {
                val groups = visible.groupBy { it.tag }.entries.sortedByDescending { it.value.last().timestampMillis }
                groups.take(maxRendered).forEach { (tag, lines) ->
                    ConsoleTagRow(tag, lines) {
                        deckState?.navigateTo(tag) { ConsoleViewer(buffer, title = "$title · $tag", fixedTag = tag) }
                    }
                }
            }
            else -> {
                visible.asReversed().take(maxRendered).forEach { line -> ConsoleLineRow(line, showTag = fixedTag == null) }
                if (visible.size >
                    maxRendered
                ) {
                    TextItem("Showing the latest $maxRendered of ${visible.size} lines. Search or filter to narrow down.")
                }
            }
        }
    }
}

private val LEVEL_FILTERS = listOf(ConsoleLevel.Verbose, ConsoleLevel.Debug, ConsoleLevel.Info, ConsoleLevel.Warn, ConsoleLevel.Error)

private fun shareLines(context: Context, title: String, lines: List<ConsoleLine>) {
    val directory = File(ScreenCapture.captureDirectory(context), "reports").apply { mkdirs() }
    val file = File(directory, "${title.filter { it.isLetterOrDigit() }.lowercase().ifEmpty { "logs" }}_${System.currentTimeMillis()}.txt")
    file.writeText(ConsoleFormat.export(lines))
    context.shareFiles(listOf(file), subject = title)
}
