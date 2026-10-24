package io.devdeck

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.devdeck.internal.DeckIcons
import io.devdeck.performance.PerformanceMonitor
import io.devdeck.performance.PerformanceOverlay
import io.devdeck.simulation.SimulationBadge
import io.devdeck.tools.KeylineOverlay
import io.devdeck.tools.UiTools
import kotlin.math.roundToInt

@Composable
internal fun DevDeckOverlay(
    drawerContent: @Composable DeckScope.() -> Unit,
    modifier: Modifier = Modifier,
    state: DeckState = rememberDeckState(),
    enabled: Boolean = true,
    side: DeckSide = DeckSide.End,
    gestures: DeckGestures = DeckGestures(),
    trigger: DeckTrigger = DeckTrigger.FloatingButton(),
    style: DeckStyle = DeckStyle(),
) {
    LaunchedEffect(enabled) {
        if (!enabled) state.snapClosed()
    }
    if (!enabled) return

    LaunchedEffect(state.targetValue, state.animationToken) {
        if (state.isDragging) return@LaunchedEffect
        val target = if (state.isOpen) 1f else 0f
        if (state.progress == target) return@LaunchedEffect
        animate(
            initialValue = state.progress,
            targetValue = target,
            animationSpec = tween(style.animationDurationMillis),
        ) { value, _ -> state.progress = value }
    }

    BackHandler(enabled = state.isOpen) {
        if (!state.navigateBack()) state.close()
    }

    val isRight = (side == DeckSide.End) == (LocalLayoutDirection.current == LayoutDirection.Ltr)

    DeckTheme(style) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val panelWidth = min(maxWidth * style.widthFraction, style.maxWidth)
            val panelWidthPx = with(LocalDensity.current) { panelWidth.toPx() }
            val containerWidthPx = constraints.maxWidth.toFloat()
            val direction = if (isRight) -1f else 1f
            val dragState = rememberDraggableState { delta -> state.onDrag(direction * delta / panelWidthPx) }
            val dragModifier = Modifier.draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = { state.onDragStarted() },
                onDragStopped = { velocity -> state.onDragStopped(direction * velocity / panelWidthPx) },
            )

            if (UiTools.isKeylineOverlayEnabled) {
                KeylineOverlay()
            }

            if (PerformanceMonitor.isOverlayEnabled) {
                PerformanceOverlay()
            }

            SimulationBadge()

            if (state.isVisible) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (gestures.swipeToClose) dragModifier else Modifier),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind { drawRect(style.scrimColor, alpha = state.progress) }
                            .pointerInput(state) { detectTapGestures { state.close() } },
                    )
                    DeckPanel(
                        state = state,
                        style = style,
                        isRight = isRight,
                        drawerContent = drawerContent,
                        modifier = Modifier
                            .offset {
                                val visibleWidth = panelWidthPx * state.progress
                                val x = if (isRight) containerWidthPx - visibleWidth else visibleWidth - panelWidthPx
                                IntOffset(x.roundToInt(), 0)
                            }
                            .width(panelWidth)
                            .fillMaxHeight(),
                    )
                }
            } else if (trigger is DeckTrigger.FloatingButton) {
                FloatingTrigger(trigger = trigger, onClick = state::open)
            }
        }
    }
}

@Composable
private fun FloatingTrigger(trigger: DeckTrigger.FloatingButton, onClick: () -> Unit) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(trigger.alignment)
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .then(
                    if (trigger.draggable) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offset += dragAmount
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                trigger.icon?.invoke() ?: Icon(DeckIcons.BugReport, contentDescription = "Open debug menu")
            }
        }
    }
}
