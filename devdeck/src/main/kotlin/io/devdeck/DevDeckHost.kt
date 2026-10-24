package io.devdeck

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.devdeck.gesture.inputObserver
import io.devdeck.gesture.twoFingerSwipe
import io.devdeck.internal.findActivity
import io.devdeck.plugin.DeckPlugin
import io.devdeck.plugin.DeckPluginContent
import io.devdeck.plugin.DeckPluginRegistry

@Composable
public fun DevDeckHost(
    drawerContent: @Composable DeckScope.() -> Unit,
    modifier: Modifier = Modifier,
    state: DeckState = rememberDeckState(),
    enabled: Boolean = true,
    side: DeckSide = DeckSide.End,
    gestures: DeckGestures = DeckGestures(),
    trigger: DeckTrigger = DeckTrigger.FloatingButton(),
    style: DeckStyle = DeckStyle(),
    content: @Composable () -> Unit,
) {
    val isRight = (side == DeckSide.End) == (LocalLayoutDirection.current == LayoutDirection.Ltr)
    val openDeck = remember(state) { { state.open() } }
    HostActivityEffect(if (enabled) gestures.keyboardShortcut else null, state)
    Box(modifier.twoFingerSwipe(enabled && gestures.twoFingerSwipe, towardsLeft = isRight, onSwipe = openDeck)) {
        content()
        DevDeckOverlay(
            drawerContent = drawerContent,
            modifier = Modifier.matchParentSize(),
            state = state,
            enabled = enabled,
            side = side,
            gestures = gestures,
            trigger = trigger,
            style = style,
        )
    }
}

@Composable
public fun DevDeckHost(
    plugins: List<DeckPlugin>,
    modifier: Modifier = Modifier,
    state: DeckState = rememberDeckState(),
    enabled: Boolean = true,
    side: DeckSide = DeckSide.End,
    gestures: DeckGestures = DeckGestures(),
    trigger: DeckTrigger = DeckTrigger.FloatingButton(),
    style: DeckStyle = DeckStyle(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(plugins) { DeckPluginRegistry.attach(plugins, context) }
    DevDeckHost(
        drawerContent = { DeckPluginContent(plugins, collapsibleSections = style.collapsibleSections) },
        modifier = modifier,
        state = state,
        enabled = enabled,
        side = side,
        gestures = gestures,
        trigger = trigger,
        style = style,
        content = content,
    )
}

@Composable
private fun HostActivityEffect(shortcut: DeckShortcut?, state: DeckState) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, shortcut, state) {
        if (activity == null) return@DisposableEffect onDispose {}
        HostedActivities.add(activity)
        DevDeck.removeOverlay(activity)
        val observer = if (shortcut != null) activity.inputObserver() else null
        val handler: (KeyEvent) -> Boolean = { event -> shortcut.handle(event, state) }
        observer?.keyListeners?.add(handler)
        onDispose {
            observer?.keyListeners?.remove(handler)
            HostedActivities.remove(activity)
        }
    }
}
