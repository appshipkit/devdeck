package io.devdeck.network

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import io.devdeck.components.FilterPill
import io.devdeck.components.FilterPillRow
import io.devdeck.components.SearchField
import io.devdeck.components.SegmentedPills
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons

public enum class NetworkView(public val label: String) { Timeline("Timeline"), ByEndpoint("By endpoint") }

@Composable
public fun NetworkInspector(
    modifier: Modifier = Modifier,
    highlights: List<NetworkHighlight> = emptyList(),
    initialView: NetworkView = NetworkView.Timeline,
    maxItems: Int = 150,
) {
    val calls by NetworkLogger.calls.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var view by rememberSaveable { mutableStateOf(initialView) }
    val visible = remember(calls, query, filter, highlights) {
        calls.filter { call ->
            val matchesFilter = when (filter) {
                FILTER_ALL -> true
                FILTER_ERRORS -> call.isFailure
                else -> highlights.firstOrNull { it.label == filter }?.matches?.invoke(call) == true
            }
            matchesFilter &&
                (
                    query.isBlank() ||
                        listOfNotNull(call.url, call.method, call.operationName, call.responseCode?.toString())
                            .any { it.contains(query.trim(), ignoreCase = true) }
                    )
        }
    }
    val groups = remember(visible) { visible.groupByEndpoint() }
    val repeatCounts = remember(calls) { calls.groupingBy { it.groupKey }.eachCount() }
    val failures = remember(calls) { calls.count { it.isFailure } }
    val endpoints = remember(calls) { calls.distinctBy { it.groupKey }.size }

    Column(modifier.fillMaxWidth()) {
        Text(
            "${calls.size} requests · $endpoints endpoints · $failures failed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        SegmentedPills(
            options = NetworkView.entries,
            selected = view,
            onSelected = { view = it },
            optionLabel = { it.label },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        SearchField(query = query, onQueryChange = { query = it }, placeholder = "Search URL, operation, status")
        FilterPillRow(verticalAlignment = Alignment.CenterVertically) {
            FilterPill(text = "All ${calls.size}", selected = filter == FILTER_ALL, onClick = { filter = FILTER_ALL })
            FilterPill(
                text = "Errors $failures",
                selected = filter == FILTER_ERRORS,
                onClick = { filter = FILTER_ERRORS },
                color = ErrorColor,
                showDot = true,
            )
            highlights.forEach { highlight ->
                FilterPill(
                    text = highlight.label,
                    selected = filter == highlight.label,
                    onClick = { filter = highlight.label },
                    color = highlight.color,
                    showDot = true,
                )
            }
            IconButton(onClick = NetworkLogger::clear, enabled = calls.isNotEmpty()) {
                Icon(
                    DeckIcons.Delete,
                    contentDescription = "Clear requests",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = if (calls.isEmpty()) 0.38f else 1f),
                )
            }
        }
        when {
            visible.isEmpty() -> TextItem(if (calls.isEmpty()) "No requests yet" else "No requests match")
            view == NetworkView.Timeline -> visible.take(maxItems).forEach { call ->
                NetworkCallCard(call, highlights, repeatCount = repeatCounts[call.groupKey] ?: 1)
            }
            else -> groups.take(maxItems).forEach { group -> NetworkGroupCard(group, highlights) }
        }
    }
}

private const val FILTER_ALL = "__all"
private const val FILTER_ERRORS = "__errors"

private val ErrorColor = Color(0xFFD32F2F)
