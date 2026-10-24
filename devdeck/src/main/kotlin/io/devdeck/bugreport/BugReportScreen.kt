package io.devdeck.bugreport

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.capture.ScreenCapture
import io.devdeck.components.ButtonItem
import io.devdeck.components.SectionDivider
import io.devdeck.components.SectionTitle
import io.devdeck.components.SwitchItem
import io.devdeck.components.TextItem
import io.devdeck.console.ConsoleFormat
import io.devdeck.device.appVersionInfo
import io.devdeck.device.deviceInfo
import io.devdeck.internal.shareFiles
import io.devdeck.logcat.LogcatReader
import io.devdeck.logs.DeckLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
public fun BugReportScreen(modifier: Modifier = Modifier, buildInfo: () -> Map<String, String> = { emptyMap() }) {
    val context = LocalContext.current
    var description by rememberSaveable { mutableStateOf("") }
    var includeLogs by rememberSaveable { mutableStateOf(true) }
    var includeDeviceInfo by rememberSaveable { mutableStateOf(true) }
    val captures = remember(ScreenCapture.galleryVersion) { ScreenCapture.captures(context) }
    val selected = remember { mutableStateListOf<File>() }

    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("What went wrong?") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SwitchItem("Include logs", checked = includeLogs, onCheckedChange = { includeLogs = it })
        SwitchItem("Include device and app info", checked = includeDeviceInfo, onCheckedChange = { includeDeviceInfo = it })

        SectionTitle("Attachments")
        if (captures.isEmpty()) TextItem("Take a screenshot or recording first to attach it")
        captures.forEach { file ->
            val checked = file in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(checked, role = Role.Checkbox) { if (it) selected.add(file) else selected.remove(file) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        SectionDivider()
        ButtonItem(
            title = "Share report",
            onClick = {
                val report = buildReport(context, description, includeDeviceInfo, buildInfo())
                val attachments = buildList {
                    addAll(selected.filter { it.exists() })
                    if (includeLogs) add(writeLogFile(context))
                }
                context.shareFiles(attachments, text = report, subject = "Bug report: ${context.packageName}")
            },
        )
    }
}

private fun buildReport(context: Context, description: String, includeDeviceInfo: Boolean, buildInfo: Map<String, String>): String =
    buildString {
        appendLine(description.ifBlank { "(no description)" })
        if (includeDeviceInfo) {
            appendLine()
            appendLine("App")
            appendLine("• Package: ${context.packageName}")
            appVersionInfo(context)?.let { appendLine("• Version: ${it.versionName} (${it.versionCode})") }
            buildInfo.forEach { (key, value) -> appendLine("• $key: $value") }
            appendLine()
            appendLine("Device")
            deviceInfo(context).forEach { (key, value) -> appendLine("• $key: $value") }
        }
    }

private fun writeLogFile(context: Context): File {
    val directory = File(ScreenCapture.captureDirectory(context), "reports").apply { mkdirs() }
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    val file = File(directory, "logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt")
    val customLogs = DeckLogger.entries.value.reversed().joinToString(separator = "\n") { entry ->
        val payload = entry.payload?.let { "\n    $it" }.orEmpty()
        "${formatter.format(Date(entry.timestampMillis))} [${entry.tag}] ${entry.message}$payload"
    }
    val logcat = LogcatReader.buffer.lines.value.takeLast(LOGCAT_LINES_IN_REPORT)
    file.writeText(
        buildString {
            appendLine("=== DevDeck logs ===")
            appendLine(customLogs.ifEmpty { "(none)" })
            if (logcat.isNotEmpty()) {
                appendLine()
                appendLine("=== Logcat (last ${logcat.size} lines) ===")
                append(ConsoleFormat.export(logcat))
            }
        },
    )
    return file
}

private const val LOGCAT_LINES_IN_REPORT = 1_000
