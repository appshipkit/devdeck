package io.devdeck.device

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.BulletItem

@Composable
public fun AppInfo(modifier: Modifier = Modifier, showSystemShortcuts: Boolean = false) {
    val context = LocalContext.current
    val app = remember(context) { appVersionInfo(context) }
    Column(modifier.fillMaxWidth()) {
        BulletItem(label = "Package", value = context.packageName)
        if (app != null) BulletItem(label = "Version", value = "${app.versionName} (${app.versionCode})")
        if (showSystemShortcuts) {
            AppSettingsButton()
            DeveloperOptionsButton()
        }
    }
}
