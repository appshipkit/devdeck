package io.devdeck.plugin

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.devdeck.components.ConfirmButtonItem
import io.devdeck.components.PageLinkItem
import io.devdeck.crash.CrashList
import io.devdeck.crash.CrashReporter

public class CrashPlugin(private val testCrashButton: Boolean = false, override val section: String? = "Logs 📚") : DeckPlugin {

    override val id: String = "crashes"

    override fun onAttach(context: Context) {
        CrashReporter.install(context)
    }

    @Composable
    override fun Content() {
        val count = remember(CrashReporter.version) { CrashReporter.crashes().size }
        PageLinkItem("Crash logs", subtitle = if (count == 0) "No crashes" else "$count saved") { CrashList() }
        if (testCrashButton) {
            ConfirmButtonItem(title = "Trigger test crash", onConfirm = { throw IllegalStateException("Test crash from the debug drawer") })
        }
    }
}
