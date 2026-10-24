package io.devdeck.plugin

import androidx.compose.runtime.Composable
import io.devdeck.components.ExpandableItem
import io.devdeck.components.SwitchItem
import io.devdeck.performance.MemoryInfo
import io.devdeck.performance.PerformanceMonitor

public class PerformancePlugin(
    private val overlay: Boolean = true,
    private val memory: Boolean = true,
    override val section: String? = "System 📱",
) : DeckPlugin {

    override val id: String = "performance"

    @Composable
    override fun Content() {
        if (overlay) {
            SwitchItem(
                title = "Frame rate & memory overlay",
                checked = PerformanceMonitor.isOverlayEnabled,
                onCheckedChange = { PerformanceMonitor.isOverlayEnabled = it },
            )
        }
        if (memory) ExpandableItem("Memory") { MemoryInfo() }
    }
}
