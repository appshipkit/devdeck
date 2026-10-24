package io.devdeck.capture

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.FOCUS_SCROLL_DELAY_MILLIS
import io.devdeck.LocalDeckState
import io.devdeck.components.ButtonItem
import io.devdeck.plugin.UI_TOOLS_PLUGIN_ID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
public fun ScreenRecordButton(title: String = "Record a video", stopTitle: String = "Stop recording") {
    val context = LocalContext.current
    val drawerState = LocalDeckState.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) return@rememberLauncherForActivityResult
        drawerState?.close()
        ScreenCapture.scope.launch {
            if (drawerState != null) snapshotFlow { drawerState.isVisible }.first { !it }
            ScreenCapture.startRecording(context.applicationContext, result.resultCode, data)
        }
    }
    // The header's recording chip asks the drawer to show this button.
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(drawerState?.pendingFocus) {
        if (drawerState?.pendingFocus == RECORDING_FOCUS) {
            delay(FOCUS_SCROLL_DELAY_MILLIS)
            bringIntoView.bringIntoView()
            drawerState.pendingFocus = null
        }
    }
    val focusModifier = Modifier.bringIntoViewRequester(bringIntoView)
    if (ScreenCapture.isRecording) {
        ButtonItem(
            title = stopTitle,
            destructive = true,
            modifier = focusModifier,
            onClick = { ScreenCapture.stopRecording(context) },
        )
    } else {
        ButtonItem(title = title, modifier = focusModifier, onClick = {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            launcher.launch(manager.createScreenCaptureIntent())
        })
    }
}

/** Asks the drawer to open the screen capture tools and show the record button. */
internal const val RECORDING_FOCUS = "$UI_TOOLS_PLUGIN_ID/recording"
