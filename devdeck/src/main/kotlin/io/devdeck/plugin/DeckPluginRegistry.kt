package io.devdeck.plugin

import android.content.Context

internal object DeckPluginRegistry {
    private val attached = mutableMapOf<String, DeckPlugin>()

    fun attach(plugins: List<DeckPlugin>, context: Context, reattachReplaced: Boolean = false) {
        val appContext = context.applicationContext
        plugins.forEach { plugin ->
            val shouldAttach = synchronized(attached) {
                val previous = attached[plugin.id]
                val replace = previous == null || (reattachReplaced && previous !== plugin)
                if (replace) attached[plugin.id] = plugin
                replace
            }
            if (shouldAttach) plugin.onAttach(appContext)
        }
    }
}
