package io.devdeck.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
internal fun SectionCard(
    title: String,
    collapsible: Boolean,
    modifier: Modifier = Modifier,
    closeOthersOnOpen: Boolean = false,
    pluginIds: List<String> = emptyList(),
    content: @Composable () -> Unit,
) {
    val drawerState = LocalDeckState.current
    val key = "$SECTION_KEY_PREFIX$title"
    var localExpanded by rememberSaveable(title) { mutableStateOf(false) }
    val expanded = !collapsible || (drawerState?.expandedRows?.get(key) ?: localExpanded)
    val reveal = rememberRevealOnExpand()
    val toggle = {
        if (drawerState != null) {
            if (!expanded && closeOthersOnOpen) {
                drawerState.expandedRows.keys
                    .filter { it.startsWith(SECTION_KEY_PREFIX) && it != key }
                    .forEach { drawerState.expandedRows[it] = false }
            }
            drawerState.expandedRows[key] = !expanded
        } else {
            localExpanded = !expanded
        }
        if (!expanded) reveal.request()
    }
    // Opens this section when something asks the drawer to show one of its plugins.
    // A request is a plugin id, optionally followed by "/" and something inside that plugin.
    val focusHere = drawerState?.pendingFocus?.let { focus -> pluginIds.any { focus == it || focus.startsWith("$it/") } } == true
    LaunchedEffect(focusHere) { if (focusHere && !expanded) toggle() }
    val (emoji, name) = remember(title) { splitSectionTitle(title) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "sectionChevron")

    DeckCard(modifier.then(reveal.modifier)) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (collapsible) Modifier.clickable(onClick = toggle) else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionBadge(name, emoji)
            Spacer(Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (collapsible) {
                Icon(
                    DeckIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse $name" else "Expand $name",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                InfoGroup(content = content)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private const val SECTION_KEY_PREFIX = "section/"
