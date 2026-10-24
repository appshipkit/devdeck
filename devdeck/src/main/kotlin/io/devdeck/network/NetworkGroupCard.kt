package io.devdeck.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.components.Pill

@Composable
internal fun NetworkGroupCard(group: NetworkGroup, highlights: List<NetworkHighlight>) {
    val deckState = LocalDeckState.current
    val matched = remember(group, highlights) { highlights.filter { highlight -> group.calls.any(highlight.matches) } }
    val accent = matched.firstOrNull()?.color ?: if (group.failures > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    CardRow(
        accent = accent,
        onClick = { deckState?.navigateTo(group.title) { NetworkGroupDetail(group.key, highlights = highlights) } },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(group.method, methodColor(group.method))
            Pill("${group.count} ${if (group.count == 1) "call" else "calls"}", CallCountColor)
            if (group.failures > 0) Pill("${group.failures} failed", Color(0xFFD32F2F))
            if (group.pending > 0) Pill("${group.pending} pending", Color(0xFF9E9E9E), filled = false)
            matched.forEach { Pill(it.label, it.color, filled = false) }
        }
        Text(
            group.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (group.isGraphQl) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOf(
                group.host,
                "avg ${NetworkFormat.duration(group.averageDurationMillis)}",
                "last ${NetworkFormat.status(group.latest)} at ${NetworkFormat.time(group.latest.startedAtMillis)}",
            ).filter { it.isNotEmpty() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
