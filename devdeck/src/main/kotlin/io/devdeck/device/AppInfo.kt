package io.devdeck.device

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.BulletItem
import io.devdeck.components.InfoGroup

@Composable
public fun AppInfo(modifier: Modifier = Modifier, showSystemShortcuts: Boolean = false) {
    val context = LocalContext.current
    val rows = remember(context) { appRows(context) }
    Column(modifier.fillMaxWidth()) {
        InfoGroup {
            rows.forEach { (label, value) -> BulletItem(label = label, value = value) }
        }
        if (showSystemShortcuts) {
            AppSettingsButton()
            DeveloperOptionsButton()
        }
    }
}
