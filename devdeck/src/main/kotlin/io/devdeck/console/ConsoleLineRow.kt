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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.components.Pill
import io.devdeck.internal.isDeckDark

@Composable
internal fun ConsoleLineRow(line: ConsoleLine, showTag: Boolean) {
    var expanded by rememberSaveable(line.id) { mutableStateOf(false) }
    var overflows by remember(line.id) { mutableStateOf(false) }
    val expandable = expanded || overflows
    val isDark = isDeckDark()
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (expandable) {
                    Modifier
                        .clickable(onClickLabel = if (expanded) "Collapse" else "Expand") { expanded = !expanded }
                        .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(line.level.letter.toString(), line.level.color(isDark))
            Text(
                ConsoleFormat.time(line.timestampMillis),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showTag) {
                Text(
                    line.tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            line.message,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (line.level >= ConsoleLevel.Error) line.level.color(isDark) else MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
    }
}

private const val COLLAPSED_LINES = 3
