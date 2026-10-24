package io.devdeck.breadcrumbs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.devdeck.components.FilterPill
import io.devdeck.components.FilterPillRow
import io.devdeck.components.InfoGroup
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.shareFiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
public fun BreadcrumbList(modifier: Modifier = Modifier, maxRendered: Int = 200) {
    val context = LocalContext.current
    val all by Breadcrumbs.entries.collectAsState()
    var filter by rememberSaveable { mutableStateOf<BreadcrumbType?>(null) }
    val visible = remember(all, filter) { all.filter { filter == null || it.type == filter }.asReversed() }
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (visible.size == 1) "1 step" else "${visible.size} steps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { context.shareFiles(emptyList(), text = Breadcrumbs.format(visible.asReversed()), subject = "Breadcrumbs") },
                enabled = visible.isNotEmpty(),
            ) { Icon(DeckIcons.Share, contentDescription = "Share breadcrumbs") }
            IconButton(onClick = Breadcrumbs::clear, enabled = all.isNotEmpty()) {
                Icon(
                    DeckIcons.Delete,
                    contentDescription = "Clear breadcrumbs",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = if (all.isEmpty()) 0.38f else 1f),
                )
            }
        }
        FilterPillRow {
            FilterPill(text = "All", selected = filter == null, onClick = { filter = null })
            BreadcrumbType.entries.forEach { type ->
                FilterPill(text = type.label, selected = filter == type, onClick = { filter = type }, color = type.color, showDot = true)
            }
        }
        if (visible.isEmpty()) TextItem("Nothing recorded yet. Open screens, tap around or send requests.")
        InfoGroup(dividerThreshold = 2) {
            visible.take(maxRendered).forEach { BreadcrumbRow(it, formatter.format(Date(it.timestampMillis))) }
        }
    }
}

@Composable
private fun BreadcrumbRow(crumb: Breadcrumb, time: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            time,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(Modifier.padding(top = 5.dp).size(8.dp).background(crumb.type.color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(crumb.type.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = crumb.type.color)
            Text(crumb.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

internal val BreadcrumbType.color: Color
    get() = when (this) {
        BreadcrumbType.Screen -> Color(0xFF1E88E5)
        BreadcrumbType.Tap -> Color(0xFF8E24AA)
        BreadcrumbType.Network -> Color(0xFF00897B)
        BreadcrumbType.App -> Color(0xFFF57C00)
        BreadcrumbType.Custom -> Color(0xFF757575)
    }
