package io.devdeck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.components.DeckCard
import io.devdeck.device.appVersionInfo
import io.devdeck.internal.DeckIcons
import io.devdeck.simulation.MemorySimulator
import io.devdeck.simulation.NetworkSimulator
import io.devdeck.simulation.mb

@Composable
internal fun DeckPanel(
    state: DeckState,
    style: DeckStyle,
    isRight: Boolean,
    drawerContent: @Composable DeckScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = if (isRight) {
        AbsoluteRoundedCornerShape(topLeft = 20.dp, bottomLeft = 20.dp)
    } else {
        AbsoluteRoundedCornerShape(topRight = 20.dp, bottomRight = 20.dp)
    }
    val scope = remember(state) { DeckScopeImpl(state) }
    val insetSides = WindowInsetsSides.Vertical + if (isRight) WindowInsetsSides.Right else WindowInsetsSides.Left
    val colors = LocalDeckColors.current

    Surface(modifier = modifier, shape = shape, color = colors?.panel ?: MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        CompositionLocalProvider(LocalDeckState provides state) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(insetSides))) {
                val page = state.pages.lastOrNull()
                if (page == null) {
                    DeckHeader(title = style.title, subtitle = style.subtitle, onClose = state::close)
                } else {
                    DeckPageBar(title = page.title, onBack = { state.navigateBack() }, onClose = state::close)
                }
                key(page) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 4.dp, bottom = 16.dp),
                    ) {
                        if (page == null) {
                            scope.drawerContent()
                        } else {
                            DeckCard(Modifier.padding(horizontal = 12.dp)) {
                                Spacer(Modifier.height(4.dp))
                                page.content(scope)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckHeader(title: String, subtitle: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { appVersionInfo(context)?.versionName }
    val warning = LocalDeckColors.current?.warning ?: MaterialTheme.colorScheme.error
    val network = NetworkSimulator.simulation
    val heldBytes = MemorySimulator.heldBytes
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DeckIcons.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CloseButton(onClose)
        }
        val chips = buildList {
            version?.let { add(HeaderChip("v$it", null)) }
            if (network.isActive) add(HeaderChip(network.summary, warning))
            if (heldBytes > 0) add(HeaderChip("Holding ${mb(heldBytes)}", warning))
            if (MemorySimulator.killOnBackground) add(HeaderChip("Kill on background", warning))
        }
        if (chips.isNotEmpty()) {
            Row(
                Modifier.padding(top = 12.dp, end = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chips.forEach { Chip(it) }
            }
        }
    }
}

@Composable
private fun DeckPageBar(title: String, onBack: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(DeckIcons.ArrowBack, contentDescription = "Back") }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        CloseButton(onClose)
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val card = LocalDeckColors.current?.card ?: MaterialTheme.colorScheme.surfaceVariant
    IconButton(onClick = onClose) {
        Box(Modifier.size(32.dp).background(card, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                DeckIcons.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private data class HeaderChip(val text: String, val accent: Color?)

@Composable
private fun Chip(chip: HeaderChip) {
    val shape = RoundedCornerShape(50)
    val card = LocalDeckColors.current?.card ?: MaterialTheme.colorScheme.surface
    val accent = chip.accent
    Text(
        text = chip.text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .background(accent?.copy(alpha = 0.14f) ?: card, shape)
            .then(if (accent == null) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
