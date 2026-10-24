package io.devdeck.plugin

import androidx.compose.runtime.Composable
import io.devdeck.components.PageLinkItem
import io.devdeck.device.DeveloperOptionsButton
import io.devdeck.device.PermissionsList

public class DevicePlugin(
    // Device info moved to InfoPlugin (see its deviceInfo switch); kept so existing calls still compile.
    @Suppress("UNUSED_PARAMETER") deviceInfo: Boolean = true,
    private val permissions: Boolean = true,
    private val developerOptions: Boolean = true,
    override val section: String? = "System 📱",
) : DeckPlugin {

    override val id: String = "device"

    @Composable
    override fun Content() {
        if (permissions) PageLinkItem("Permissions") { PermissionsList() }
        if (developerOptions) DeveloperOptionsButton()
    }
}
