package io.devdeck.storage

import android.content.Context
import java.io.File

internal object StorageFiles {

    fun sharedPreferencesNames(context: Context): List<String> = File(context.applicationInfo.dataDir, "shared_prefs")
        .listFiles { file -> file.extension == "xml" }
        .orEmpty()
        .map { it.nameWithoutExtension }
        .sorted()

    fun usage(context: Context): List<Pair<String, Long>> = listOfNotNull(
        "Files" to context.filesDir.sizeOnDisk(),
        "Cache" to context.cacheDir.sizeOnDisk(),
        "Databases" to File(context.applicationInfo.dataDir, "databases").sizeOnDisk(),
        "Shared preferences" to File(context.applicationInfo.dataDir, "shared_prefs").sizeOnDisk(),
        context.getExternalFilesDir(null)?.let { "External files" to it.sizeOnDisk() },
        context.externalCacheDir?.let { "External cache" to it.sizeOnDisk() },
    )

    private fun File.sizeOnDisk(): Long = if (!exists()) 0L else walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
