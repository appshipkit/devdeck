package io.devdeck.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.devdeck.LocalDeckState
import io.devdeck.components.ExpandableItem
import io.devdeck.components.expandableKey

@Composable
public fun ScreenCaptureTools(modifier: Modifier = Modifier, title: String = "Screen capture tools") {
    val drawerState = LocalDeckState.current
    LaunchedEffect(drawerState?.pendingFocus) {
        if (drawerState?.pendingFocus == RECORDING_FOCUS) drawerState.expandedRows[expandableKey(drawerState, title)] = true
    }
    ExpandableItem(title = title, modifier = modifier) {
        ScreenshotButton()
        ScreenRecordButton()
        GalleryButton()
    }
}
