package io.devdeck.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.devdeck.DeckScope
import io.devdeck.LocalDeckState

@Composable
public fun PageLinkItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    pageTitle: String = title,
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
    }
}
