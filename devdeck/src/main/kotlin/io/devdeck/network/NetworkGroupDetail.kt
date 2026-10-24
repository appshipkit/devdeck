package io.devdeck.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.devdeck.components.BulletItem
import io.devdeck.components.InfoGroup
import io.devdeck.components.SectionDivider
import io.devdeck.components.SectionTitle
import io.devdeck.components.TextItem

@Composable
internal fun NetworkGroupDetail(groupKey: String, modifier: Modifier = Modifier, highlights: List<NetworkHighlight> = emptyList()) {
    val calls by NetworkLogger.calls.collectAsState()
    val group = remember(calls, groupKey) { calls.filter { it.groupKey == groupKey }.groupByEndpoint().firstOrNull() }
    if (group == null) {
        TextItem("These requests are no longer in the log")
        return
    }
    val statusCounts = remember(group) {
        group.calls.groupingBy { NetworkFormat.status(it) }.eachCount().toSortedMap()
    }
    Column(modifier.fillMaxWidth()) {
        InfoGroup {
            BulletItem("Requests", group.count.toString())
            BulletItem("Succeeded", (group.count - group.failures - group.pending).toString())
            BulletItem("Failed", group.failures.toString())
            if (group.pending > 0) BulletItem("Pending", group.pending.toString())
            BulletItem("Status codes", statusCounts.entries.joinToString("  ") { (status, count) -> "$status ×$count" })
            BulletItem(
                "Duration",
                "avg ${NetworkFormat.duration(
                    group.averageDurationMillis,
                )} · min ${NetworkFormat.duration(group.minDurationMillis)} · max ${NetworkFormat.duration(group.maxDurationMillis)}",
            )
            BulletItem("First call", NetworkFormat.dateTime(group.calls.last().startedAtMillis))
            BulletItem("Last call", NetworkFormat.dateTime(group.latest.startedAtMillis))
        }
        SectionDivider()
        SectionTitle("All calls (newest first)")
        group.calls.take(MAX_CALLS_SHOWN).forEach { call -> NetworkCallCard(call, highlights, showTitle = false) }
        if (group.count > MAX_CALLS_SHOWN) TextItem("…and ${group.count - MAX_CALLS_SHOWN} older calls")
    }
}

private const val MAX_CALLS_SHOWN = 100
