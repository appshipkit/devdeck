package io.devdeck.plugin

import androidx.compose.runtime.Composable
import io.devdeck.bugreport.BugReportButton
import io.devdeck.components.ExpandableItem
import io.devdeck.components.PageLinkItem
import io.devdeck.device.AppInfo
import io.devdeck.device.AppVersionItem
import io.devdeck.device.DeviceInfo
import io.devdeck.device.DeviceSnapshotCard

public class InfoPlugin(
    private val snapshot: Boolean = true,
    private val bugReport: Boolean = true,
    private val version: Boolean = true,
    private val versionText: String? = null,
    private val buildInfo: () -> Map<String, String> = { emptyMap() },
    override val section: String? = "Info ℹ️",
    private val appInfo: Boolean = true,
    private val deviceInfo: Boolean = true,
) : DeckPlugin {

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        snapshot: Boolean = true,
        bugReport: Boolean = true,
        version: Boolean = true,
        versionText: String? = null,
        buildInfo: () -> Map<String, String> = { emptyMap() },
        section: String? = "Info ℹ️",
    ) : this(snapshot, bugReport, version, versionText, buildInfo, section, appInfo = true, deviceInfo = true)

    override val id: String = "info"

    @Composable
    override fun Content() {
        if (appInfo) ExpandableItem("App info") { AppInfo() }
        if (deviceInfo) ExpandableItem("Device info") { DeviceInfo() }
        if (snapshot) {
            PageLinkItem(title = "Device & app snapshot", subtitle = "Share versions and device details as an image") {
                DeviceSnapshotCard(buildInfo = buildInfo)
            }
        }
        if (bugReport) BugReportButton(buildInfo = buildInfo)
        if (version) AppVersionItem(text = versionText)
    }
}
