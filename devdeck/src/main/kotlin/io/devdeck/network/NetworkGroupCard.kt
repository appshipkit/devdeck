package io.devdeck.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.components.Pill
import io.devdeck.internal.isDeckDark

@Composable
internal fun NetworkGroupCard(group: NetworkGroup, highlights: List<NetworkHighlight>) {
    val deckState = LocalDeckState.current
    val isDark = isDeckDark()
    val matched = remember(group, highlights) { highlights.filter { highlight -> group.calls.any(highlight.matches) } }
    val accent = matched.firstOrNull()?.color ?: if (group.failures > 0) NetworkColors.error(isDark) else NetworkColors.success(isDark)
    CardRow(
        accent = accent,
        onClick = { deckState?.navigateTo(group.title) { NetworkGroupDetail(group.key, highlights = highlights) } },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(group.method, methodColor(group.method, isDark))
            Pill("${group.count} ${if (group.count == 1) "call" else "calls"}", NetworkColors.callCount(isDark))
            if (group.failures > 0) Pill("${group.failures} failed", NetworkColors.error(isDark))
            if (group.pending > 0) Pill("${group.pending} pending", NetworkColors.pending(isDark), filled = false)
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
