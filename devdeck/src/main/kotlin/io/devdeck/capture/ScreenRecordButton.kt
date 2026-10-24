package io.devdeck.capture

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import io.devdeck.LocalDeckState
import io.devdeck.components.BulletItem
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
    if (ScreenCapture.isRecording) {
        BulletItem(stopTitle) { ScreenCapture.stopRecording(context) }
    } else {
        BulletItem(title) {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            launcher.launch(manager.createScreenCaptureIntent())
        }
    }
}
