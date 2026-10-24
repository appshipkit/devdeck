package io.devdeck.bugreport

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.breadcrumbs.Breadcrumbs
import io.devdeck.capture.CaptureTransfer
import io.devdeck.capture.ScreenCapture
import io.devdeck.components.ButtonItem
import io.devdeck.components.SectionDivider
import io.devdeck.components.SectionTitle
import io.devdeck.components.SwitchItem
import io.devdeck.components.TextItem
import io.devdeck.console.ConsoleFormat
import io.devdeck.device.appVersionInfo
import io.devdeck.device.deviceInfo
import io.devdeck.internal.findActivity
import io.devdeck.internal.shareFiles
import io.devdeck.logcat.LogcatReader
import io.devdeck.logs.DeckLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
public fun BugReportScreen(modifier: Modifier = Modifier, buildInfo: () -> Map<String, String> = { emptyMap() }) {
    val context = LocalContext.current
    var description by rememberSaveable { mutableStateOf("") }
    var includeLogs by rememberSaveable { mutableStateOf(true) }
    var includeDeviceInfo by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val captures by produceState(emptyList<File>(), context, ScreenCapture.galleryVersion) {
        value = withContext(Dispatchers.IO) { ScreenCapture.captures(context) }
    }
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
        if (CaptureTransfer.canDragBetweenApps) {
            AttachmentDropZone(onDropped = { files -> selected.addAll(files.filterNot { it in selected }) })
        }
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
                val extraInfo = buildInfo()
                val chosen = selected.toList()
                val text = description
                val withDeviceInfo = includeDeviceInfo
                val withLogs = includeLogs
                scope.launch {
                    val (report, attachments) = withContext(Dispatchers.IO) {
                        buildReport(context, text, withDeviceInfo, extraInfo) to buildList {
                            addAll(chosen.filter { it.exists() })
                            if (withLogs) add(writeLogFile(context))
                        }
                    }
                    context.shareFiles(attachments, text = report, subject = "Bug report: ${context.packageName}")
                }
            },
        )
    }
}

/** Accepts images and videos dragged from another app or a computer, copies them to the gallery and attaches them. */
@Composable
private fun AttachmentDropZone(onDropped: (List<File>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hovering by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val target = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                dragging = true
            }

            override fun onEntered(event: DragAndDropEvent) {
                hovering = true
            }

            override fun onExited(event: DragAndDropEvent) {
                hovering = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                hovering = false
                dragging = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val activity = context.findActivity() ?: return false
                val dragEvent = event.toAndroidDragEvent()
                scope.launch {
                    val files = CaptureTransfer.importDrop(activity, dragEvent)
                    if (files.isEmpty()) Toast.makeText(context, "Couldn't read the dropped file", Toast.LENGTH_SHORT).show()
                    onDropped(files)
                }
                return true
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = if (hovering) "Release to attach" else "Drop images or videos here to attach them",
        style = MaterialTheme.typography.bodySmall,
        color = if (dragging) colors.primary else colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .dragAndDropTarget(shouldStartDragAndDrop = { CaptureTransfer.isMediaDrag(it.toAndroidDragEvent()) }, target = target)
            .background(if (hovering) colors.primary.copy(alpha = 0.12f) else Color.Transparent, shape)
            .border(1.dp, if (dragging) colors.primary else colors.outlineVariant, shape)
            .padding(vertical = 14.dp, horizontal = 12.dp),
    )
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
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    val customLogs = DeckLogger.entries.value.reversed().joinToString(separator = "\n") { entry ->
        val payload = entry.payload?.let { "\n    $it" }.orEmpty()
        "${formatter.format(Date(entry.timestampMillis))} [${entry.tag}] ${entry.message}$payload"
    }
    val logcat = LogcatReader.buffer.lines.value.takeLast(LOGCAT_LINES_IN_REPORT)
    return ReportFiles.write(
        context,
        name = "logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt",
        text = buildString {
            appendLine("=== Recent steps ===")
            appendLine(Breadcrumbs.format(Breadcrumbs.recent(STEPS_IN_REPORT)).ifEmpty { "(none)" })
            appendLine()
            appendLine("=== DevDeck logs ===")
            appendLine(customLogs.ifEmpty { "(none)" })
            if (logcat.isNotEmpty()) {
                appendLine()
                appendLine("=== Logcat (last ${logcat.size} lines) ===")
                append(ConsoleFormat.export(logcat))
            }
        },
    )
}

private const val LOGCAT_LINES_IN_REPORT = 1_000
private const val STEPS_IN_REPORT = 100
