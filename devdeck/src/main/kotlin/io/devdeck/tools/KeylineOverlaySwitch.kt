package io.devdeck.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.devdeck.components.SwitchItem

@Composable
public fun KeylineOverlaySwitch(modifier: Modifier = Modifier, title: String = "Keyline overlay") {
    SwitchItem(
        title = title,
        checked = UiTools.isKeylineOverlayEnabled,
        onCheckedChange = { UiTools.isKeylineOverlayEnabled = it },
        modifier = modifier,
    )
}
