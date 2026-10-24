package io.devdeck.capture

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckColors
import io.devdeck.internal.DeckIcons
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Small floating bar with the recording time, pause/resume and stop, so testers don't have to reopen the drawer. */
@Composable
internal fun RecordingControls(modifier: Modifier = Modifier) {
    if (!ScreenCapture.isRecording) return
    val context = LocalContext.current
    val elapsed = rememberRecordingElapsed()
    val paused = ScreenCapture.isRecordingPaused
    val isDark = LocalDeckColors.current?.isDark == true
    val background = if (isDark) Color(0xE6202124) else Color(0xEBFFFFFF)
    val content = if (isDark) Color(0xFFE8EAED) else Color(0xFF202124)
    BoxWithConstraints(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 8.dp)) {
        var barSize by remember { mutableStateOf(IntSize.Zero) }
        // Keep the bar fully on screen: it starts top-centre, so it can move half the spare width either way.
        val maxX = ((constraints.maxWidth - barSize.width) / 2f).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - barSize.height).toFloat().coerceAtLeast(0f)
        fun clamp(offset: Offset) = Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(0f, maxY))
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { barSize = it }
                .absoluteOffset {
                    val offset = clamp(ControlsPosition.offset)
                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                }
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        ControlsPosition.offset = clamp(ControlsPosition.offset + amount)
                    }
                }
                .shadow(4.dp, CircleShape)
                .background(background, CircleShape)
                .padding(start = 12.dp, end = 4.dp)
                .semantics { contentDescription = if (paused) "Recording paused" else "Recording" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(if (paused) content.copy(alpha = 0.4f) else RecordingRed, CircleShape))
            Text(
                formatRecordingTime(elapsed),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = content,
            )
            if (ScreenCapture.canPauseRecording) {
                IconButton(
                    onClick = { if (paused) ScreenCapture.resumeRecording(context) else ScreenCapture.pauseRecording(context) },
                    modifier = Modifier.size(ControlSize),
                ) {
                    Icon(
                        if (paused) DeckIcons.PlayArrow else DeckIcons.Pause,
                        contentDescription = if (paused) "Resume recording" else "Pause recording",
                        tint = content,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            IconButton(onClick = { ScreenCapture.stopRecording(context) }, modifier = Modifier.size(ControlSize)) {
                Icon(DeckIcons.Stop, contentDescription = "Stop recording", tint = RecordingRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Shared across screens and drawer openings, so the bar stays where the tester moved it.
private object ControlsPosition {
    var offset by mutableStateOf(Offset.Zero)
}

/** Recording time in millis, ticking while a recording is running. */
@Composable
internal fun rememberRecordingElapsed(): Long {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val ticking = ScreenCapture.isRecording && !ScreenCapture.isRecordingPaused
    LaunchedEffect(ticking) {
        while (ticking) {
            now = SystemClock.elapsedRealtime()
            delay(TICK_MILLIS)
        }
    }
    return ScreenCapture.recordingElapsedMillis(now)
}

internal fun formatRecordingTime(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

internal val RecordingRed = Color(0xFFE53935)

private val ControlSize = 36.dp
private const val TICK_MILLIS = 500L
