package io.devdeck.console

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.components.Pill
import io.devdeck.network.CallCountColor

@Composable
internal fun ConsoleTagRow(tag: String, lines: List<ConsoleLine>, onClick: () -> Unit) {
    val errors = lines.count { it.level >= ConsoleLevel.Error }
    val warnings = lines.count { it.level == ConsoleLevel.Warn }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("${lines.size} ${if (lines.size == 1) "line" else "lines"}", CallCountColor)
            if (errors > 0) Pill("$errors E", ConsoleLevel.Error.color)
            if (warnings > 0) Pill("$warnings W", ConsoleLevel.Warn.color)
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
