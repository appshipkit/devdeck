package io.devdeck.bugreport

import android.content.Context
import java.io.File

/** Exported reports and log files live in the app cache and only the newest few are kept. */
internal object ReportFiles {

    fun write(context: Context, name: String, text: String): File {
        val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
        val file = File(directory, name)
        file.writeText(text)
        directory.listFiles()
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .drop(MAX_FILES)
            .forEach { if (it != file) it.delete() }
        return file
    }

    private const val DIRECTORY = "devdeck-reports"
    private const val MAX_FILES = 20
}
