package io.devdeck

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.capture.RECORDING_FOCUS
import io.devdeck.capture.RecordingRed
import io.devdeck.capture.ScreenCapture
import io.devdeck.capture.formatRecordingTime
import io.devdeck.capture.rememberRecordingElapsed
import io.devdeck.components.DeckCard
import io.devdeck.device.appVersionInfo
import io.devdeck.internal.DeckIcons
import io.devdeck.plugin.UPDATES_PLUGIN_ID
import io.devdeck.simulation.MemorySimulator
import io.devdeck.simulation.NetworkSimulator
import io.devdeck.simulation.mb
import io.devdeck.updates.UpdateChecker
import io.devdeck.updates.updateAvailableColor
import kotlinx.coroutines.delay

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
            // Whatever asked to be shown has had time to scroll into view; drop the request so it can't linger.
            LaunchedEffect(state.pendingFocus) {
                if (state.pendingFocus != null) {
                    delay(FOCUS_TIMEOUT_MILLIS)
                    state.pendingFocus = null
                }
            }
            val page = state.pages.lastOrNull()
            AnimatedContent(
                targetState = PageEntry(page, state.pages.size),
                transitionSpec = {
                    val forward = targetState.depth > initialState.depth
                    val enter = slideInHorizontally(tween(PAGE_TRANSITION_MILLIS)) { if (forward) it else -it / 3 }
                    val exit = slideOutHorizontally(tween(PAGE_TRANSITION_MILLIS)) { if (forward) -it / 3 else it }
                    (enter togetherWith exit).apply { targetContentZIndex = if (forward) 1f else -1f }
                },
                contentKey = { it.page },
                label = "DeckPage",
            ) { entry ->
                DeckPageContent(
                    entry.page,
                    state,
                    style,
                    scope,
                    colors?.panel ?: MaterialTheme.colorScheme.surface,
                    insetSides,
                    drawerContent,
                )
            }
        }
    }
}

private data class PageEntry(val page: DeckPage?, val depth: Int)

@Composable
private fun DeckPageContent(
    page: DeckPage?,
    state: DeckState,
    style: DeckStyle,
    scope: DeckScope,
    background: Color,
    insetSides: WindowInsetsSides,
    drawerContent: @Composable DeckScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(background).windowInsetsPadding(WindowInsets.safeDrawing.only(insetSides))) {
        if (page == null) {
            DeckHeader(title = style.title, subtitle = style.subtitle, onClose = state::close)
        } else {
            DeckPageBar(title = page.title, onBack = { state.navigateBack() }, onClose = state::close)
        }
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

private const val PAGE_TRANSITION_MILLIS = 280
private const val FOCUS_TIMEOUT_MILLIS = 2_000L

@Composable
private fun DeckHeader(title: String, subtitle: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { appVersionInfo(context)?.let { "${it.versionName} (${it.versionCode})" } }
    val warning = LocalDeckColors.current?.warning ?: MaterialTheme.colorScheme.error
    LaunchedEffect(Unit) { UpdateChecker.checkIfDue() }
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
                if (version != null) {
                    Text(
                        text = "Version $version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HeaderIconButton(DeckIcons.Settings, "Open app settings") { openAppSettings(context) }
            CloseButton(onClose)
        }
        val update = UpdateChecker.availableUpdate
        val updateColor = updateAvailableColor(LocalDeckColors.current?.isDark == true)
        val deckState = LocalDeckState.current
        val recordingElapsed = if (ScreenCapture.isRecording) rememberRecordingElapsed() else 0L
        val chips = buildList {
            if (ScreenCapture.isRecording) {
                val label = if (ScreenCapture.isRecordingPaused) "Recording paused" else "Recording"
                add(
                    HeaderChip(
                        "$label · ${formatRecordingTime(recordingElapsed)}",
                        RecordingRed,
                        onClick = { deckState?.pendingFocus = RECORDING_FOCUS },
                    ),
                )
            }
            if (update != null) {
                add(HeaderChip("New version · ${update.label}", updateColor, onClick = { deckState?.pendingFocus = UPDATES_PLUGIN_ID }))
            }
            if (network.isActive) add(HeaderChip(network.summary, warning))
            if (heldBytes > 0) add(HeaderChip("Holding ${mb(heldBytes)}", warning))
            if (MemorySimulator.killOnBackground) add(HeaderChip("Kill when backgrounded", warning))
        }
        if (chips.isNotEmpty()) {
            Row(Modifier.padding(top = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Stays put while the chips scroll, so stopping a recording is always one tap.
                if (ScreenCapture.isRecording) {
                    StopRecordingButton(onClick = { ScreenCapture.stopRecording(context) })
                    Spacer(Modifier.width(6.dp))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.forEach { Chip(it) }
                }
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
private fun HeaderIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val card = LocalDeckColors.current?.card ?: MaterialTheme.colorScheme.surfaceVariant
    IconButton(onClick = onClick, modifier = Modifier.size(HeaderButtonSize)) {
        Box(Modifier.size(32.dp).background(card, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    try {
        context.startActivity(if (context is Activity) intent else intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val card = LocalDeckColors.current?.card ?: MaterialTheme.colorScheme.surfaceVariant
    IconButton(onClick = onClose, modifier = Modifier.size(HeaderButtonSize)) {
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

// 40dp keeps the touch target close to the guideline while pulling the settings and close buttons together.
private val HeaderButtonSize = 40.dp

private data class HeaderChip(val text: String, val accent: Color?, val onClick: (() -> Unit)? = null)

@Composable
private fun StopRecordingButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(RecordingRed)
            .clickable(role = Role.Button, onClickLabel = "Stop recording", onClick = onClick)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(10.dp).background(Color.White, RoundedCornerShape(2.dp)))
        Text(
            "Stop",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

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
            .clip(shape)
            .then(chip.onClick?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
