package io.devdeck.capture

import android.app.Activity
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
    val capture = rememberCaptureAfterClosing()
    ButtonItem(title = title, onClick = {
        capture { activity ->
            val file = ScreenCapture.takeScreenshot(activity)
            if (file != null) "Screenshot saved" else "Screenshot failed"
        }
    })
}

@Composable
public fun LongScreenshotButton(title: String = "Take a long screenshot") {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val capture = rememberCaptureAfterClosing()
    ButtonItem(title = title, subtitle = "The whole scrolling screen in one image", onClick = {
        capture { activity ->
            when (ScreenCapture.longScreenshot(activity)) {
                is ScreenCapture.LongScreenshotOutcome.Saved -> "Long screenshot saved"
                ScreenCapture.LongScreenshotOutcome.NothingToScroll -> "Nothing to scroll on this screen"
                ScreenCapture.LongScreenshotOutcome.Failed -> "Long screenshot failed"
            }
        }
    })
}

/** Closes the drawer, waits for it to leave the screen, runs the capture and shows the message it returns. */
@Composable
private fun rememberCaptureAfterClosing(): (suspend (Activity) -> String) -> Unit {
    val context = LocalContext.current
    val drawerState = LocalDeckState.current
    return { block ->
        context.findActivity()?.let { activity ->
            drawerState?.close()
            ScreenCapture.scope.launch {
                if (drawerState != null) snapshotFlow { drawerState.isVisible }.first { !it }
                delay(CAPTURE_SETTLE_MILLIS)
                Toast.makeText(activity, block(activity), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
