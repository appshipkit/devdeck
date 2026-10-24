package io.devdeck.internal

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.io.File

internal object DeckSettings {

    private const val NAME = "devdeck_settings"

    private val legacyFiles = listOf(
        "devdeck_appearance",
        "devdeck_endpoint",
        "devdeck_network_simulator",
        "devdeck_updates",
        "devdeck_deeplinks",
        "devdeck_permissions",
    )

    @Volatile
    private var cleaned = false

    fun of(context: Context): SharedPreferences {
        val app = context.applicationContext
        if (!cleaned) {
            cleaned = true
            removeLegacyFiles(app)
        }
        return app.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    private fun removeLegacyFiles(context: Context) {
        legacyFiles.forEach { name ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(name)
            } else {
                File(context.applicationInfo.dataDir, "shared_prefs/$name.xml").delete()
            }
        }
    }
}
