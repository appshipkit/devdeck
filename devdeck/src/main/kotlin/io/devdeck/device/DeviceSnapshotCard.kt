package io.devdeck.device

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.devdeck.capture.ScreenCapture
import io.devdeck.internal.copyToClipboard
import io.devdeck.internal.shareFiles
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
public fun DeviceSnapshotCard(modifier: Modifier = Modifier, buildInfo: () -> Map<String, String> = { emptyMap() }) {
    val context = LocalContext.current
    val snapshot = remember(context) { systemSnapshot(context, buildInfo()) }
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SnapshotImage(
            snapshot = snapshot,
            modifier = Modifier.drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val file = saveSnapshot(context, layer) ?: return@launch
                    context.shareFiles(listOf(file), text = snapshot.toText(), subject = "${snapshot.appName} device & app info")
                }
            }) { Text("Share image") }
            OutlinedButton(onClick = {
                scope.launch {
                    val saved = saveSnapshot(context, layer) != null
                    Toast.makeText(
                        context,
                        if (saved) "Saved to the capture gallery" else "Could not save image",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }) { Text("Save") }
            TextButton(onClick = { context.copyToClipboard("Device & app info", snapshot.toText()) }) { Text("Copy text") }
        }
    }
}

@Composable
private fun SnapshotImage(snapshot: SystemSnapshot, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(SnapshotBackground, shape)
            .border(1.dp, SnapshotBorder, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(snapshot.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SnapshotText)
        Text("Device & app snapshot", style = MaterialTheme.typography.bodyMedium, color = SnapshotMuted)
        snapshot.sections.forEach { section ->
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = SnapshotBorder)
            Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SnapshotAccent)
            section.rows.forEach { (label, value) ->
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = SnapshotMuted)) { append("$label  ") }
                        withStyle(SpanStyle(color = SnapshotText)) { append(value) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = SnapshotBorder)
        Text(
            "Generated ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(snapshot.generatedAtMillis))} · DevDeck",
            style = MaterialTheme.typography.labelSmall,
            color = SnapshotMuted,
        )
    }
}

private suspend fun saveSnapshot(context: Context, layer: GraphicsLayer): File? = runCatching {
    val bitmap = layer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    val file = ScreenCapture.newCaptureFile(context, "snapshot", "png")
    withContext(Dispatchers.IO) { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    ScreenCapture.notifyGalleryChanged()
    file
}.getOrNull()

private val SnapshotBackground = Color.White
private val SnapshotBorder = Color(0xFFE0E0E0)
private val SnapshotText = Color(0xFF1C1B1F)
private val SnapshotMuted = Color(0xFF5F6368)
private val SnapshotAccent = Color(0xFF6750A4)
