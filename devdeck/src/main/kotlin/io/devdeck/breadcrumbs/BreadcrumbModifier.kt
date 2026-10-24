package io.devdeck.breadcrumbs

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

public fun Modifier.breadcrumb(label: String): Modifier = composed {
    val currentLabel by rememberUpdatedState(label)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (waitForUpOrCancellation(PointerEventPass.Initial) != null) Breadcrumbs.tap(currentLabel)
        }
    }
}
