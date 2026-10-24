package io.devdeck.console

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.components.Pill
import io.devdeck.internal.isDeckDark
import io.devdeck.internal.plural
import io.devdeck.network.NetworkColors

@Composable
internal fun ConsoleTagRow(tag: String, lines: List<ConsoleLine>, onClick: () -> Unit) {
    val errors = lines.count { it.level >= ConsoleLevel.Error }
    val warnings = lines.count { it.level == ConsoleLevel.Warn }
    val isDark = isDeckDark()
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(plural(lines.size, "line"), NetworkColors.callCount(isDark))
            if (errors > 0) Pill("$errors E", ConsoleLevel.Error.color(isDark))
            if (warnings > 0) Pill("$warnings W", ConsoleLevel.Warn.color(isDark))
        }
        Text(
            tag,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "last at ${ConsoleFormat.time(lines.last().timestampMillis)} · ${lines.last().message.lineSequence().first()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
