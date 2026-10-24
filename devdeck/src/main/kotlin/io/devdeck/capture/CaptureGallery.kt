package io.devdeck.capture

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.components.BulletItem
import io.devdeck.components.TextItem
import io.devdeck.internal.openFile
import io.devdeck.internal.shareFiles
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
public fun GalleryButton(title: String = "Open the gallery") {
    val drawerState = LocalDeckState.current
    BulletItem(title) { drawerState?.navigateTo("Gallery") { CaptureGallery() } }
}

@Composable
public fun CaptureGallery(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val files = remember(ScreenCapture.galleryVersion) { ScreenCapture.captures(context) }
    Column(modifier.fillMaxWidth()) {
        if (files.isEmpty()) TextItem("No screenshots or recordings yet")
        files.forEach { file -> GalleryRow(file) }
    }
}

@Composable
private fun GalleryRow(file: File) {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { loadThumbnail(file) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { context.openFile(file) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val image = thumbnail
            if (image != null) {
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
            } else {
                Text(if (file.extension == "mp4") "▶" else "…", style = MaterialTheme.typography.titleLarge)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row {
                TextButton(onClick = { context.shareFiles(listOf(file)) }) { Text("Share") }
                TextButton(onClick = {
                    file.delete()
                    ScreenCapture.notifyGalleryChanged()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun loadThumbnail(file: File): ImageBitmap? = runCatching {
    when (file.extension) {
        "png" -> {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val sample = (maxOf(bounds.outWidth, bounds.outHeight) / THUMBNAIL_SIZE_PX).coerceAtLeast(1)
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }

        "mp4" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.media.ThumbnailUtils.createVideoThumbnail(file, android.util.Size(THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX), null)
                .asImageBitmap()
        } else {
            null
        }

        else -> null
    }
}.getOrNull()
