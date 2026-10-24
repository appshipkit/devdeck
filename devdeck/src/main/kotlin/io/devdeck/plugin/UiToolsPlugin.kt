package io.devdeck.plugin

import androidx.compose.runtime.Composable
import io.devdeck.capture.ScreenCaptureTools
import io.devdeck.tools.AnimationSpeedSwitch
import io.devdeck.tools.KeylineOverlaySwitch

public class UiToolsPlugin(
    private val animationSpeed: Boolean = true,
    private val keylineOverlay: Boolean = true,
    private val screenCapture: Boolean = true,
    override val section: String? = "User Interface 🎨",
) : DeckPlugin {

    override val id: String = "ui_tools"

    @Composable
    override fun Content() {
        if (animationSpeed) AnimationSpeedSwitch()
        if (keylineOverlay) KeylineOverlaySwitch()
        if (screenCapture) ScreenCaptureTools()
    }
}
