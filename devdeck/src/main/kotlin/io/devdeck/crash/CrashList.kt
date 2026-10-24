package io.devdeck.crash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.devdeck.components.BulletItem
import io.devdeck.components.CodeBlock
import io.devdeck.components.ConfirmButtonItem
import io.devdeck.components.PageLinkItem
import io.devdeck.components.SectionDivider
import io.devdeck.components.TextItem
import io.devdeck.internal.copyToClipboard
import io.devdeck.internal.shareFiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
public fun CrashList(modifier: Modifier = Modifier) {
    val crashes = remember(CrashReporter.version) { CrashReporter.crashes() }
    Column(modifier.fillMaxWidth()) {
        if (crashes.isEmpty()) TextItem("No crashes recorded. Crashes are saved here and shown after the app restarts.")
        crashes.forEach { crash ->
            PageLinkItem(
                title = crash.title.substringBefore(':').substringAfterLast('.'),
                subtitle = "${formatTime(crash.timestampMillis)} · ${crash.title.substringAfter(": ", "").take(80)}",
            ) { CrashDetail(crash) }
        }
        if (crashes.isNotEmpty()) {
            SectionDivider()
            ConfirmButtonItem(title = "Clear crash logs", onConfirm = CrashReporter::clear)
        }
    }
}

@Composable
private fun CrashDetail(crash: CrashRecord) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        BulletItem("Time", formatTime(crash.timestampMillis))
        BulletItem("Thread", crash.thread)
        Row(Modifier.padding(horizontal = 8.dp)) {
            TextButton(onClick = { context.copyToClipboard("Stack trace", crash.stackTrace) }) { Text("Copy") }
            TextButton(onClick = { context.shareFiles(emptyList(), text = crash.stackTrace, subject = crash.title) }) { Text("Share") }
        }
        CodeBlock(crash.stackTrace, label = "Stack trace", collapsedLines = 40, copyable = false)
    }
}

private fun formatTime(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))
