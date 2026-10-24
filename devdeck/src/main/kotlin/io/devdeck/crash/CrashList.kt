package io.devdeck.crash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.devdeck.components.BulletItem
import io.devdeck.components.CodeBlock
import io.devdeck.components.InfoGroup
import io.devdeck.components.PageLinkItem
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.shareFiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
public fun CrashList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val crashes = remember(CrashReporter.version) { CrashReporter.crashes() }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (crashes.size == 1) "1 crash" else "${crashes.size} crashes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { context.shareFiles(crashes.map { it.file }, subject = "Crash logs") },
                enabled = crashes.isNotEmpty(),
            ) { Icon(DeckIcons.Share, contentDescription = "Share crash logs") }
            IconButton(onClick = CrashReporter::clear, enabled = crashes.isNotEmpty()) {
                Icon(
                    DeckIcons.Delete,
                    contentDescription = "Clear crash logs",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = if (crashes.isEmpty()) 0.38f else 1f),
                )
            }
        }
        if (crashes.isEmpty()) TextItem("No crashes recorded. Crashes are saved here and shown after the app restarts.")
        InfoGroup(dividerThreshold = 2) {
            crashes.forEach { crash ->
                PageLinkItem(
                    title = crash.title.substringBefore(':').substringAfterLast('.'),
                    subtitle = "${formatTime(crash.timestampMillis)} · ${crash.title.substringAfter(": ", "").take(80)}",
                ) { CrashDetail(crash) }
            }
        }
    }
}

@Composable
private fun CrashDetail(crash: CrashRecord) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BulletItem("Time", formatTime(crash.timestampMillis))
                BulletItem("Thread", crash.thread)
            }
            IconButton(onClick = { context.shareFiles(listOf(crash.file), text = crash.stackTrace, subject = crash.title) }) {
                Icon(DeckIcons.Share, contentDescription = "Share crash")
            }
        }
        crash.steps?.let { CodeBlock(it, label = "Steps before the crash", collapsedLines = 12, copyable = true) }
        CodeBlock(crash.stackTrace, label = "Stack trace", collapsedLines = 40, copyable = true)
    }
}

private fun formatTime(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))
