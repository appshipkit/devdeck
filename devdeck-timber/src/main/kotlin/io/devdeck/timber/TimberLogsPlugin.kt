package io.devdeck.timber

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.devdeck.components.PageLinkItem
import io.devdeck.console.ConsoleLevel
import io.devdeck.console.ConsoleView
import io.devdeck.console.ConsoleViewer
import io.devdeck.plugin.DeckPlugin
import timber.log.Timber

public class TimberLogsPlugin(
    private val title: String = "Timber logs",
    private val plantTree: Boolean = true,
    private val initialView: ConsoleView = ConsoleView.ByTag,
    override val section: String? = "Logs 📚",
) : DeckPlugin {

    override val id: String = "timber_logs"

    override fun onAttach(context: Context) {
        if (plantTree && Timber.forest().none { it is DevDeckTimberTree }) Timber.plant(DevDeckTimberTree())
    }

    @Composable
    override fun Content() {
        val lines by TimberLogs.buffer.lines.collectAsState()
        val errors = lines.count { it.level >= ConsoleLevel.Error }
        PageLinkItem(
            title = title,
            subtitle = if (lines.isEmpty()) "No entries" else "${lines.size} lines · $errors errors",
        ) { ConsoleViewer(TimberLogs.buffer, title = title, initialView = initialView) }
    }
}
