package io.devdeck.plugin

import android.content.Context
import androidx.compose.runtime.Composable
import io.devdeck.updates.UpdateChecker
import io.devdeck.updates.UpdateNotifier
import io.devdeck.updates.UpdateSource
import io.devdeck.updates.UpdatesPanel

internal const val UPDATES_PLUGIN_ID = "updates"

public class UpdatesPlugin(
    private val source: UpdateSource,
    private val checkOnStart: Boolean = true,
    private val checkIntervalMillis: Long = UpdateChecker.DEFAULT_CHECK_INTERVAL_MILLIS,
    private val notify: Boolean = false,
    override val section: String? = "App 🛠️",
) : DeckPlugin {

    override val id: String = UPDATES_PLUGIN_ID

    override fun onAttach(context: Context) {
        val app = context.applicationContext
        UpdateChecker.attach(app, source, checkIntervalMillis)
        UpdateChecker.notifier = if (notify) ({ UpdateNotifier.show(app, it) }) else null
        if (checkOnStart) UpdateChecker.checkIfDue()
    }

    @Composable
    override fun Content() {
        UpdatesPanel()
    }
}
