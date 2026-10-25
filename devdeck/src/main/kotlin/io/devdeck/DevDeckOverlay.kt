package io.devdeck

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.content.edit
import io.devdeck.capture.RecordingControls
import io.devdeck.capture.ScreenCapture
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.DeckSettings
import io.devdeck.performance.PerformanceMonitor
import io.devdeck.performance.PerformanceOverlay
import io.devdeck.simulation.SimulationBadge
import io.devdeck.tools.KeylineOverlay
import io.devdeck.tools.UiTools
import io.devdeck.updates.UpdateChecker
import io.devdeck.updates.updateAvailableColor
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

    val isVisible by remember(state) { derivedStateOf { state.isVisible } }
    DisposableEffect(isVisible) {
        if (isVisible) DeckVisibility.shown()
        onDispose { if (isVisible) DeckVisibility.hidden() }
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

            if (!ScreenCapture.isCapturing) SimulationBadge()

            if (!isVisible && !ScreenCapture.isCapturing) RecordingControls()

            if (isVisible) {
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
                if (!ScreenCapture.isCapturing) FloatingTrigger(trigger = trigger, onClick = state::open)
            }
        }
    }
}

@Composable
private fun FloatingTrigger(trigger: DeckTrigger.FloatingButton, onClick: () -> Unit) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
    ) {
        val sizePx = with(LocalDensity.current) { TriggerSize.roundToPx() }
        val rangeX = (constraints.maxWidth - sizePx).coerceAtLeast(1).toFloat()
        val rangeY = (constraints.maxHeight - sizePx).coerceAtLeast(1).toFloat()
        val default = remember(trigger.alignment, layoutDirection) {
            val spot = trigger.alignment.align(IntSize.Zero, IntSize(FRACTION_SCALE, FRACTION_SCALE), layoutDirection)
            Offset(spot.x.toFloat() / FRACTION_SCALE, spot.y.toFloat() / FRACTION_SCALE)
        }
        // Stored as a fraction of the free area, so the button keeps its place across screens, rotation and restarts.
        val fraction = (if (trigger.draggable) TriggerPosition.get(context) else null) ?: default
        val position = IntOffset((fraction.x * rangeX).roundToInt(), (fraction.y * rangeY).roundToInt())
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 4.dp,
            // Absolute (left-based) placement: the default spot is already mirrored for RTL and drags are physical.
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset { position }
                .then(
                    if (trigger.draggable) {
                        Modifier.pointerInput(rangeX, rangeY, default) {
                            detectDragGestures(onDragEnd = { TriggerPosition.save(context) }) { change, dragAmount ->
                                change.consume()
                                val current = TriggerPosition.get(context) ?: default
                                TriggerPosition.set(
                                    Offset(
                                        (current.x + dragAmount.x / rangeX).coerceIn(0f, 1f),
                                        (current.y + dragAmount.y / rangeY).coerceIn(0f, 1f),
                                    ),
                                )
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .size(TriggerSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                trigger.icon?.invoke() ?: Icon(DeckIcons.BugReport, contentDescription = "Open debug menu")
            }
        }
        if (UpdateChecker.availableUpdate != null) {
            Box(Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset { position }.size(TriggerSize)) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(2.dp)
                        .background(updateAvailableColor(isSystemInDarkTheme()), CircleShape),
                )
            }
        }
    }
}

/** Where the user dragged the floating button, shared by every screen and kept in DevDeck's settings. */
private object TriggerPosition {
    private var position by mutableStateOf<Offset?>(null)
    private var loaded = false

    fun get(context: Context): Offset? {
        if (!loaded) {
            loaded = true
            val prefs = DeckSettings.of(context)
            if (prefs.contains(KEY_X)) position = Offset(prefs.getFloat(KEY_X, 0f), prefs.getFloat(KEY_Y, 0f))
        }
        return position
    }

    fun set(value: Offset) {
        position = value
    }

    fun save(context: Context) {
        val value = position ?: return
        DeckSettings.of(context).edit { putFloat(KEY_X, value.x).putFloat(KEY_Y, value.y) }
    }

    private const val KEY_X = "trigger.x"
    private const val KEY_Y = "trigger.y"
}

private val TriggerSize = 44.dp
private const val FRACTION_SCALE = 10_000
