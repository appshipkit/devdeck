package io.devdeck.capture

import android.os.Build
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

@Composable
public fun LongScreenshotButton(title: String = "Take a long screenshot") {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    val drawerState = LocalDeckState.current
    ButtonItem(title = title, subtitle = "The whole scrolling screen in one image", onClick = {
        val activity = context.findActivity() ?: return@ButtonItem
        drawerState?.close()
        ScreenCapture.scope.launch {
            if (drawerState != null) snapshotFlow { drawerState.isVisible }.first { !it }
            delay(CAPTURE_SETTLE_MILLIS)
            val file = ScreenCapture.takeLongScreenshot(activity)
            val message = if (file != null) "Long screenshot saved" else "Nothing to scroll on this screen"
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    })
}
