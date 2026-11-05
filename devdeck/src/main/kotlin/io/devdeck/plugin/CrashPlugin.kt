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
        val unseen = CrashReporter.unseenCrash != null
        PageLinkItem(
            "Crash logs",
            subtitle = when {
                count == 0 -> "No crashes"
                unseen -> "$count saved · new crash since the last launch"
                else -> "$count saved"
            },
        ) { CrashList() }
        if (testCrashButton) {
            ConfirmButtonItem(title = "Trigger test crash", onConfirm = { throw IllegalStateException("Test crash from the debug drawer") })
            // Blocks the main thread; tap the screen while it's frozen and Android reports an ANR after about 5 seconds.
            ConfirmButtonItem(title = "Freeze the app (test ANR)", onConfirm = { Thread.sleep(TEST_FREEZE_MILLIS) })
        }
    }
}

private const val TEST_FREEZE_MILLIS = 20_000L
