package io.devdeck.plugin

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import io.devdeck.actions.AppActions
import io.devdeck.actions.DeepLinkLauncher
import io.devdeck.components.ButtonItem
import io.devdeck.components.ConfirmButtonItem
import io.devdeck.components.ExpandableItem
import io.devdeck.components.SwitchItem
import kotlinx.coroutines.launch

public class AppActionsPlugin(
    private val deepLinks: Boolean = true,
    private val deepLinkSuggestions: List<String> = emptyList(),
    private val webViewDebugging: Boolean = true,
    private val restart: Boolean = true,
    private val clearCache: Boolean = true,
    private val clearData: Boolean = true,
    private val kill: Boolean = true,
    override val section: String? = "App 🛠️",
) : DeckPlugin {

    override val id: String = "app_actions"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        if (deepLinks) ExpandableItem("Open a deep link") { DeepLinkLauncher(suggestions = deepLinkSuggestions) }
        if (webViewDebugging) {
            SwitchItem(
                title = "WebView debugging",
                subtitle = "Inspect WebViews from chrome://inspect",
                checked = AppActions.isWebViewDebuggingEnabled,
                onCheckedChange = { AppActions.setWebViewDebugging(it) },
            )
        }
        if (restart) ButtonItem(title = "Restart app", onClick = { AppActions.restart(context) })
        if (clearCache) {
            ConfirmButtonItem(title = "Clear cache", onConfirm = {
                scope.launch {
                    AppActions.clearCacheInBackground(context)
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                }
            })
        }
        if (clearData) {
            ConfirmButtonItem(title = "Clear app data", subtitle = "Closes the app", onConfirm = {
                AppActions.clearAppData(context)
            })
        }
        if (kill) ConfirmButtonItem(title = "Kill app", onConfirm = AppActions::killApp)
    }
}
