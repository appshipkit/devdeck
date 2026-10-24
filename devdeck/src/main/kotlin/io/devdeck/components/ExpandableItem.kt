package io.devdeck.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import io.devdeck.LocalDeckState
import io.devdeck.internal.DeckIcons

@Composable
public fun ExpandableItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val drawerState = LocalDeckState.current
    val key = "${drawerState?.pages?.lastOrNull()?.title.orEmpty()}/$title"
    var localExpanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val expanded = drawerState?.expandedRows?.get(key) ?: localExpanded
    val reveal = rememberRevealOnExpand()
    val toggle = {
        if (drawerState != null) drawerState.expandedRows[key] = !expanded else localExpanded = !expanded
        if (!expanded) reveal.request()
    }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(modifier.fillMaxWidth().then(reveal.modifier)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = toggle)
                .then(RowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TitleAndSubtitle(title, subtitle, enabled = true, modifier = Modifier.weight(1f))
            Icon(
                DeckIcons.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(expanded) { Column(content = content) }
    }
}
