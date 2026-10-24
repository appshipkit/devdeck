package io.devdeck.capture

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import io.devdeck.LocalDeckState
import io.devdeck.components.ButtonItem
import io.devdeck.internal.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
public fun ScreenshotButton(title: String = "Take a screenshot") {
    val context = LocalContext.current
    val drawerState = LocalDeckState.current
    ButtonItem(title = title, onClick = {
        val activity = context.findActivity() ?: return@ButtonItem
        drawerState?.close()
        ScreenCapture.scope.launch {
            if (drawerState != null) snapshotFlow { drawerState.isVisible }.first { !it }
            delay(CAPTURE_SETTLE_MILLIS)
            val file = ScreenCapture.takeScreenshot(activity)
            val message = if (file != null) "Screenshot saved" else "Screenshot failed"
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    })
}
