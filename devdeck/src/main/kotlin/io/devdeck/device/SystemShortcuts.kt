package io.devdeck.device

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.ButtonItem

@Composable
public fun AppSettingsButton(modifier: Modifier = Modifier, title: String = "Show app info") {
    val context = LocalContext.current
    ButtonItem(
        title = title,
        modifier = modifier,
        onClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    )
}

@Composable
public fun DeveloperOptionsButton(modifier: Modifier = Modifier, title: String = "Developer options") {
    val context = LocalContext.current
    ButtonItem(
        title = title,
        modifier = modifier,
        onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Developer options are not enabled", Toast.LENGTH_SHORT).show()
            }
        },
    )
}
