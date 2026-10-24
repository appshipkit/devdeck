package io.devdeck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

public enum class DeckValue { Closed, Open }

@Immutable
public class DeckPage internal constructor(public val title: String, internal val content: @Composable DeckScope.() -> Unit)

@Stable
public class DeckState(initialValue: DeckValue = DeckValue.Closed) {

    public var targetValue: DeckValue by mutableStateOf(initialValue)
        private set

    public val isOpen: Boolean get() = targetValue == DeckValue.Open

    public val isVisible: Boolean get() = isOpen || progress > 0f

    public val pages: List<DeckPage> get() = backStack

    internal var progress by mutableFloatStateOf(if (initialValue == DeckValue.Open) 1f else 0f)

    internal var animationToken by mutableIntStateOf(0)
        private set

    internal var isDragging = false
        private set

    private val backStack = mutableStateListOf<DeckPage>()

    internal val expandedRows = mutableStateMapOf<String, Boolean>()

    public fun open() {
        targetValue = DeckValue.Open
    }

    public fun close() {
        targetValue = DeckValue.Closed
    }

    public fun toggle() {
        if (isOpen) close() else open()
    }

    public fun navigateTo(title: String, content: @Composable DeckScope.() -> Unit) {
        backStack.add(DeckPage(title, content))
    }

    public fun navigateBack(): Boolean = backStack.removeLastOrNull() != null

    public fun navigateToRoot() {
        backStack.clear()
    }

    internal fun snapClosed() {
        targetValue = DeckValue.Closed
        progress = 0f
        isDragging = false
    }

    internal fun onDragStarted() {
        isDragging = true
        animationToken++
    }

    internal fun onDrag(deltaFraction: Float) {
        progress = (progress + deltaFraction).coerceIn(0f, 1f)
    }

    internal fun onDragStopped(velocityFraction: Float) {
        isDragging = false
        targetValue = when {
            velocityFraction > FLING_THRESHOLD -> DeckValue.Open
            velocityFraction < -FLING_THRESHOLD -> DeckValue.Closed
            progress > 0.5f -> DeckValue.Open
            else -> DeckValue.Closed
        }
        animationToken++
    }

    public companion object {
        private const val FLING_THRESHOLD = 1.5f

        internal fun saver(): Saver<DeckState, DeckValue> = Saver(
            save = { it.targetValue },
            restore = { DeckState(it) },
        )
    }
}

@Composable
public fun rememberDeckState(initialValue: DeckValue = DeckValue.Closed): DeckState =
    rememberSaveable(saver = DeckState.saver()) { DeckState(initialValue) }
