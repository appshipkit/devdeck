package io.devdeck.device

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckColors
import io.devdeck.capture.ScreenCapture
import io.devdeck.internal.DeckIcons
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

    // The actions sit on top of the snapshot but outside the recorded layer, so they never show up in the shared image.
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SnapshotImage(
            snapshot = snapshot,
            headerEndPadding = SnapshotActionSize * 3 - 10.dp,
            modifier = Modifier.drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            },
        )
        Row(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)) {
            SnapshotAction(DeckIcons.Share, "Share snapshot") {
                scope.launch {
                    val file = saveSnapshot(context, layer) ?: return@launch
                    context.shareFiles(listOf(file), text = snapshot.toText(), subject = "${snapshot.appName} device & app info")
                }
            }
            SnapshotAction(DeckIcons.Download, "Save image") {
                scope.launch {
                    val saved = saveSnapshot(context, layer) != null
                    Toast.makeText(
                        context,
                        if (saved) "Saved to the capture gallery" else "Could not save image",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            SnapshotAction(DeckIcons.ContentCopy, "Copy as text") { context.copyToClipboard("Device & app info", snapshot.toText()) }
        }
    }
}

@Composable
private fun SnapshotAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(SnapshotActionSize)) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

private val SnapshotActionSize = 40.dp

@Composable
private fun SnapshotImage(snapshot: SystemSnapshot, modifier: Modifier = Modifier, headerEndPadding: Dp = 0.dp) {
    val shape = RoundedCornerShape(12.dp)
    val palette = if (LocalDeckColors.current?.isDark ?: isSystemInDarkTheme()) DarkSnapshotPalette else LightSnapshotPalette
    Column(
        modifier
            .fillMaxWidth()
            .background(palette.background, shape)
            .border(1.dp, palette.border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            snapshot.appName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = palette.text,
            modifier = Modifier.padding(end = headerEndPadding),
        )
        Text("Device & app snapshot", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
        snapshot.sections.forEach { section ->
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = palette.border)
            Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.accent)
            section.rows.forEach { (label, value) ->
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = palette.muted)) { append("$label  ") }
                        withStyle(SpanStyle(color = palette.text)) { append(value) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = palette.border)
        Text(
            "Generated ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(snapshot.generatedAtMillis))} · DevDeck",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
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

private data class SnapshotPalette(val background: Color, val border: Color, val text: Color, val muted: Color, val accent: Color)

private val LightSnapshotPalette = SnapshotPalette(
    background = Color.White,
    border = Color(0xFFE0E0E0),
    text = Color(0xFF1C1B1F),
    muted = Color(0xFF5F6368),
    accent = Color(0xFF6750A4),
)

private val DarkSnapshotPalette = SnapshotPalette(
    background = Color(0xFF1E1E22),
    border = Color(0xFF3A3A40),
    text = Color(0xFFE6E1E5),
    muted = Color(0xFFA8A8B0),
    accent = Color(0xFFD0BCFF),
)
