package io.devdeck.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.devdeck.DeckState
import io.devdeck.LocalDeckColors
import io.devdeck.capture.RecordingRed
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.shareFiles

/** After a relaunch, a small bar at the bottom says the app crashed last time, with View, Share and dismiss. */
@Composable
internal fun CrashPrompt(state: DeckState, modifier: Modifier = Modifier) {
    val crash = CrashReporter.unseenCrash ?: return
    val context = LocalContext.current
    val isDark = LocalDeckColors.current?.isDark == true
    val background = if (isDark) Color(0xE6202124) else Color(0xEBFFFFFF)
    val content = if (isDark) Color(0xFFE8EAED) else Color(0xFF202124)
    val kind = crash.title.substringBefore(':').substringAfterLast('.')
    Box(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 16.dp)) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .shadow(4.dp, CircleShape)
                .background(background, CircleShape)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(8.dp).background(RecordingRed, CircleShape))
            Text(
                if (kind == "ANR") "App froze last time (ANR)" else "App crashed last time",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = content,
                modifier = Modifier.padding(start = 4.dp),
            )
            TextButton(onClick = {
                state.navigateToRoot()
                state.navigateTo(kind) { CrashDetail(crash) }
                state.open()
            }) { Text("View") }
            IconButton(onClick = {
                context.shareFiles(listOf(crash.file), text = crash.report, subject = crash.title)
            }, modifier = Modifier.size(36.dp)) {
                Icon(DeckIcons.Share, contentDescription = "Share crash report", tint = content, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = CrashReporter::markSeen, modifier = Modifier.size(36.dp)) {
                Icon(DeckIcons.Close, contentDescription = "Dismiss", tint = content, modifier = Modifier.size(18.dp))
            }
        }
    }
}
