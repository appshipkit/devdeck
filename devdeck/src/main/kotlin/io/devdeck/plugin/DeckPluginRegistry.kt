package io.devdeck.plugin

import android.content.Context

internal object DeckPluginRegistry {
    private val attachedIds = mutableSetOf<String>()

    fun attach(plugins: List<DeckPlugin>, context: Context) {
        val appContext = context.applicationContext
        plugins.forEach { plugin ->
            val isNew = synchronized(attachedIds) { attachedIds.add(plugin.id) }
            if (isNew) plugin.onAttach(appContext)
        }
    }
}
