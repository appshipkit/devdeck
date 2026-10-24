package io.devdeck.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.internal.DeckIcons

@Composable
public fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val drawerState = LocalDeckState.current
    val key = "section/$title"
    var localExpanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val expanded = drawerState?.expandedRows?.get(key) ?: localExpanded
    val reveal = rememberRevealOnExpand()
    val toggle = {
        if (drawerState != null) drawerState.expandedRows[key] = !expanded else localExpanded = !expanded
        if (!expanded) reveal.request()
    }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "sectionChevron")
    Column(modifier.fillMaxWidth().then(reveal.modifier)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = toggle)
                .heightIn(min = RowMinHeight)
                .padding(horizontal = HorizontalPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                DeckIcons.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(expanded) { Column(content = content) }
    }
}
