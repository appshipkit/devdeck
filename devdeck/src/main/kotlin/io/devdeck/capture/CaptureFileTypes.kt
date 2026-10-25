package io.devdeck.capture

import android.webkit.MimeTypeMap
import java.io.File

internal val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")
internal val VIDEO_EXTENSIONS = setOf("mp4", "webm", "3gp", "mkv", "mov")

internal val File.isCaptureImage: Boolean get() = extension.lowercase() in IMAGE_EXTENSIONS
internal val File.isCaptureVideo: Boolean get() = extension.lowercase() in VIDEO_EXTENSIONS

/** MIME type from the file extension, for intents. */
internal val File.mediaType: String
    get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        ?: when {
            isCaptureVideo -> "video/*"
            isCaptureImage -> "image/*"
            extension == "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
