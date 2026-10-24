package io.devdeck.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.ButtonItem
import io.devdeck.internal.copyToClipboard

@Composable
public fun AppVersionItem(modifier: Modifier = Modifier, text: String? = null) {
    val context = LocalContext.current
    val label = text ?: remember(context) {
        val app = appVersionInfo(context)
        val name = context.applicationInfo.loadLabel(context.packageManager)
        if (app == null) name.toString() else "$name v${app.versionName} (${app.versionCode})"
    }
    ButtonItem(title = label, modifier = modifier, onClick = { context.copyToClipboard("Version", label) })
}
