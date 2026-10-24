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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.components.BulletItem
import io.devdeck.components.ConfirmDeleteButton
import io.devdeck.components.InfoGroup
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.openFile
import io.devdeck.internal.shareFiles
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
public fun GalleryButton(title: String = "Open the gallery") {
    val drawerState = LocalDeckState.current
    BulletItem(title) { drawerState?.navigateTo("Gallery") { CaptureGallery() } }
}

@Composable
public fun CaptureGallery(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loaded by produceState<List<File>?>(null, ScreenCapture.galleryVersion) {
        value = withContext(Dispatchers.IO) { ScreenCapture.captures(context) }
    }
    val files = loaded.orEmpty()
    val delete: (List<File>) -> Unit = { targets ->
        scope.launch {
            withContext(Dispatchers.IO) { targets.forEach { it.delete() } }
            ScreenCapture.notifyGalleryChanged()
        }
    }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (files.size == 1) "1 capture" else "${files.size} captures",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { context.shareFiles(files, subject = "Captures") }, enabled = files.isNotEmpty()) {
                Icon(DeckIcons.Share, contentDescription = "Share all captures")
            }
            ConfirmDeleteButton(
                contentDescription = "Delete all captures",
                onConfirm = { delete(files) },
                enabled = files.isNotEmpty(),
                confirmText = "Tap again to delete",
            )
        }
        if (loaded?.isEmpty() == true) TextItem("No screenshots or recordings yet")
        InfoGroup(dividerThreshold = 2) {
            files.forEach { file -> key(file.path) { GalleryRow(file, onDelete = { delete(listOf(file)) }) } }
        }
    }
}

@Composable
private fun GalleryRow(file: File, onDelete: () -> Unit) {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { loadThumbnail(file) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Open") { context.openFile(file) }
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
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
                Text(
                    if (file.extension == "mp4") "▶" else "…",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        Text(
            file.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row {
            IconButton(onClick = { context.shareFiles(listOf(file)) }) {
                Icon(DeckIcons.Share, contentDescription = "Share ${file.name}")
            }
            ConfirmDeleteButton(contentDescription = "Delete ${file.name}", onConfirm = onDelete, confirmText = "Tap again to delete")
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
