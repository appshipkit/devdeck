package io.devdeck.gesture

import android.app.Activity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import java.util.concurrent.CopyOnWriteArrayList

internal class InputObservingWindowCallback(private val delegate: Window.Callback) : Window.Callback by delegate {

    val touchListeners = CopyOnWriteArrayList<(MotionEvent) -> Unit>()

    val keyListeners = CopyOnWriteArrayList<(KeyEvent) -> Boolean>()

    var hasGlobalListeners = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        touchListeners.forEach { it(event) }
        return delegate.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keyListeners.any { it(event) } || delegate.dispatchKeyEvent(event)
}

internal fun Activity.inputObserver(): InputObservingWindowCallback? {
    val current = window?.callback ?: return null
    return current as? InputObservingWindowCallback ?: InputObservingWindowCallback(current).also { window.callback = it }
}

internal fun MotionEvent.activePointers(): List<TouchPoint> {
    val masked = actionMasked
    if (masked == MotionEvent.ACTION_UP || masked == MotionEvent.ACTION_CANCEL) return emptyList()
    val liftedIndex = if (masked == MotionEvent.ACTION_POINTER_UP) actionIndex else -1
    return (0 until pointerCount)
        .filter { it != liftedIndex }
        .map { index -> TouchPoint(getPointerId(index).toLong(), getX(index), getY(index)) }
}
