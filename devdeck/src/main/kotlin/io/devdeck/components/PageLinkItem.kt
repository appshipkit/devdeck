package io.devdeck.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.devdeck.DeckScope
import io.devdeck.LocalDeckState
import io.devdeck.internal.DeckIcons

@Composable
public fun PageLinkItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    pageTitle: String = title,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable DeckScope.() -> Unit,
) {
    val state = LocalDeckState.current
    Row(
        modifier
            .fillMaxWidth()
            .clickable(enabled = state != null) { state?.navigateTo(pageTitle, content) }
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleAndSubtitle(title, subtitle, enabled = true, modifier = Modifier.weight(1f))
        trailing?.invoke()
        Icon(
            DeckIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp).size(20.dp),
        )
    }
}
