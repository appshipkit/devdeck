package io.devdeck.plugin

import androidx.compose.runtime.Composable
import io.devdeck.components.ExpandableItem
import io.devdeck.device.AppSettingsButton
import io.devdeck.device.DeveloperOptionsButton
import io.devdeck.device.DeviceInfo
import io.devdeck.device.PermissionsList

public class DevicePlugin(
    private val deviceInfo: Boolean = true,
    private val permissions: Boolean = true,
    private val appSettings: Boolean = true,
    private val developerOptions: Boolean = true,
    override val section: String? = "Device 📱",
) : DeckPlugin {

    override val id: String = "device"

    @Composable
    override fun Content() {
        if (deviceInfo) ExpandableItem("Device info") { DeviceInfo() }
        if (permissions) ExpandableItem("Permissions") { PermissionsList() }
        if (appSettings) AppSettingsButton()
        if (developerOptions) DeveloperOptionsButton()
    }
}
