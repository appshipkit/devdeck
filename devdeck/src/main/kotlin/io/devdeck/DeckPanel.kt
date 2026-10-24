package io.devdeck

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.devdeck.internal.DeckIcons

@Composable
internal fun DeckPanel(
    state: DeckState,
    style: DeckStyle,
    isRight: Boolean,
    drawerContent: @Composable DeckScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = if (isRight) {
        AbsoluteRoundedCornerShape(topLeft = 16.dp, bottomLeft = 16.dp)
    } else {
        AbsoluteRoundedCornerShape(topRight = 16.dp, bottomRight = 16.dp)
    }
    val scope = remember(state) { DeckScopeImpl(state) }
    val insetSides = WindowInsetsSides.Vertical + if (isRight) WindowInsetsSides.Right else WindowInsetsSides.Left

    val containerColor = when {
        style.containerColor.isSpecified -> style.containerColor
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surface
        else -> Color.White
    }
    Surface(modifier = modifier, shape = shape, color = containerColor, shadowElevation = 8.dp) {
        CompositionLocalProvider(LocalDeckState provides state) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(insetSides))) {
                val page = state.pages.lastOrNull()
                DeckTopBar(
                    title = page?.title ?: style.title,
                    subtitle = if (page == null) style.subtitle else null,
                    isRoot = page == null,
                    onBack = { state.navigateBack() },
                    onClose = state::close,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                key(page) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                    ) {
                        if (page == null) scope.drawerContent() else page.content(scope)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckTopBar(title: String, subtitle: String?, isRoot: Boolean, onBack: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = if (isRoot) Alignment.Top else Alignment.CenterVertically,
    ) {
        if (isRoot) {
            Column(Modifier.weight(1f).padding(start = 12.dp, top = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            IconButton(onClick = onBack) { Icon(DeckIcons.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(onClick = onClose) { Icon(DeckIcons.Close, contentDescription = "Close") }
    }
}
