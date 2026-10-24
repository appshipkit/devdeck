package io.devdeck.capture

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.DragEvent
import android.view.View
import android.webkit.MimeTypeMap
import androidx.annotation.ChecksSdkIntAtLeast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Moving captures in and out of DevDeck: Downloads export, drag out of the gallery and drops onto a bug report. */
internal object CaptureTransfer {

    // MediaStore.Downloads needs no storage permission, but only exists from Android 10.
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val canSaveToDownloads: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    // Global drag and drop (to other apps and a mirrored desktop) arrived in Android 7.0.
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    val canDragBetweenApps: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    /** Copies [files] to Download/DevDeck, where a computer sees them over USB. Returns how many were saved. */
    suspend fun saveToDownloads(context: Context, files: List<File>): Int = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext 0
        val resolver = context.contentResolver
        files.count { file ->
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, file.mediaType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_FOLDER")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
                resolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } } ?: return@runCatching false
                true
            }.getOrDefault(false)
        }
    }

    /** Drag flags that let another app, or a desktop showing this device, read the dragged file. */
    fun dragFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ else 0

    fun dragClip(context: Context, file: File): ClipData =
        ClipData.newUri(context.contentResolver, file.name, ScreenCapture.uriFor(context, file))

    fun isMediaDrag(event: DragEvent): Boolean {
        val description = event.clipDescription ?: return false
        return description.hasMimeType("image/*") || description.hasMimeType("video/*")
    }

    /** Copies dropped images and videos into the capture gallery and returns the new files. */
    suspend fun importDrop(activity: Activity, event: DragEvent): List<File> {
        val clip = event.clipData ?: return emptyList()
        // Content from another app is only readable while these permissions are held.
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity.requestDragAndDropPermissions(event) else null
        val uris = (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
        return try {
            withContext(Dispatchers.IO) { uris.mapNotNull { uri -> copyIntoGallery(activity, uri) } }
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) permissions?.release()
            ScreenCapture.notifyGalleryChanged()
        }
    }

    private fun copyIntoGallery(context: Context, uri: Uri): File? = runCatching {
        val type = context.contentResolver.getType(uri).orEmpty()
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            ?: if (type.startsWith("video/")) "mp4" else "png"
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val file = File(ScreenCapture.captureDirectory(context), "dropped_$stamp.$extension")
        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return null
        file
    }.getOrNull()

    private const val DOWNLOADS_FOLDER = "DevDeck"
}

internal val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")
internal val VIDEO_EXTENSIONS = setOf("mp4", "webm", "3gp", "mkv", "mov")

internal val File.isCaptureImage: Boolean get() = extension.lowercase() in IMAGE_EXTENSIONS
internal val File.isCaptureVideo: Boolean get() = extension.lowercase() in VIDEO_EXTENSIONS

/** MIME type from the file extension, for intents and MediaStore. */
internal val File.mediaType: String
    get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        ?: when {
            isCaptureVideo -> "video/*"
            isCaptureImage -> "image/*"
            extension == "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
