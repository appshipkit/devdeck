package io.devdeck.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.devdeck.components.ExpandableItem

@Composable
public fun ScreenCaptureTools(modifier: Modifier = Modifier, title: String = "Screen capture tools") {
    ExpandableItem(title = title, modifier = modifier) {
        ScreenshotButton()
        ScreenRecordButton()
        GalleryButton()
    }
}
