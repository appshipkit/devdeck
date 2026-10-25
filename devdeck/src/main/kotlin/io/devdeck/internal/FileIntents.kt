package io.devdeck.internal

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.devdeck.capture.ScreenCapture
import io.devdeck.capture.isCaptureImage
import io.devdeck.capture.isCaptureVideo
import io.devdeck.capture.mediaType
import java.io.File

internal fun Context.openFile(file: File) {
    val uri = ScreenCapture.uriFor(this, file)
    val type = file.mediaType
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, type)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, "No app can open ${file.name}", Toast.LENGTH_SHORT).show()
    }
}

internal fun Context.shareFiles(files: List<File>, text: String? = null, subject: String? = null) {
    if (files.isEmpty()) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        text?.let { intent.putExtra(Intent.EXTRA_TEXT, it) }
        subject?.let { intent.putExtra(Intent.EXTRA_SUBJECT, it) }
        startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    val uris = ArrayList(files.map { ScreenCapture.uriFor(this, it) })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    }
    intent.type = when {
        files.map { it.mediaType }.distinct().size == 1 -> files.first().mediaType
        files.all { it.isCaptureImage } -> "image/*"
        files.all { it.isCaptureVideo } -> "video/*"
        else -> "*/*"
    }
    text?.let { intent.putExtra(Intent.EXTRA_TEXT, it) }
    subject?.let { intent.putExtra(Intent.EXTRA_SUBJECT, it) }
    intent.clipData = ClipData.newRawUri(null, uris.first()).apply {
        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
