package io.devdeck.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.components.Pill
import io.devdeck.internal.isDeckDark

@Composable
internal fun NetworkCallCard(call: NetworkCall, highlights: List<NetworkHighlight>, repeatCount: Int = 1, showTitle: Boolean = true) {
    val deckState = LocalDeckState.current
    val isDark = isDeckDark()
    val matched = remember(call, highlights) { highlights.filter { it.matches(call) } }
    val accent = matched.firstOrNull()?.color ?: statusColor(call, isDark)
    CardRow(
        accent = accent,
        onClick = { deckState?.navigateTo(call.displayName) { NetworkCallDetail(call.id, highlights = highlights) } },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(call.method, methodColor(call.method, isDark))
            Pill(NetworkFormat.status(call), statusColor(call, isDark))
            if (call.simulation != null) Pill("Simulated", NetworkColors.simulated(isDark), filled = false)
            if (repeatCount > 1) Pill("×$repeatCount", MaterialTheme.colorScheme.onSurfaceVariant, filled = false)
            matched.forEach { Pill(it.label, it.color, filled = false) }
        }
        if (showTitle) {
            Text(
                call.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (call.operationName != null) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            listOf(
                call.host,
                NetworkFormat.duration(call.durationMillis),
                NetworkFormat.size(call.responseSize),
                NetworkFormat.time(call.startedAtMillis),
            )
                .filter { it.isNotEmpty() }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun CardRow(accent: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(accent, RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { content() }
    }
}
