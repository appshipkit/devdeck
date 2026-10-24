package io.devdeck.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.devdeck.components.SwitchItem

@Composable
public fun AnimationSpeedSwitch(modifier: Modifier = Modifier, title: String = "Slow down animations", slowScale: Float = 4f) {
    SwitchItem(
        title = title,
        checked = UiTools.isAnimationSlowedDown,
        onCheckedChange = { UiTools.setAnimationsSlowedDown(it, slowScale) },
        modifier = modifier,
    )
}
