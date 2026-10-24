package io.devdeck.plugin

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import io.devdeck.actions.AppActions
import io.devdeck.components.DropdownItem
import io.devdeck.internal.DeckSettings
import io.devdeck.logs.DeckLogger

public class EndpointPlugin(
    private val endpoints: List<Endpoint>,
    private val default: Endpoint = endpoints.firstOrNull() ?: error("EndpointPlugin needs at least one endpoint"),
    private val title: String = "Endpoint",
    private val key: String = DEFAULT_KEY,
    override val section: String? = "Configuration 🔧",
    private val restartOnChange: Boolean = false,
    private val onChanged: ((Endpoint) -> Unit)? = null,
) : DeckPlugin {

    override val id: String = "endpoint:$key"

    public fun current(context: Context): Endpoint = current(context, endpoints, default, key)

    @Composable
    override fun Content() {
        val context = LocalContext.current
        var selected by remember { mutableStateOf(current(context)) }
        DropdownItem(
            title = title,
            subtitle = selected.url,
            options = endpoints,
            selected = selected,
            onSelected = { endpoint ->
                selected = endpoint
                // Apps may restart the process from onChanged too, so write synchronously before handing over.
                save(context, key, endpoint, synchronous = restartOnChange || onChanged != null)
                DeckLogger.log("$title switched to ${endpoint.name}", payload = endpoint.url)
                onChanged?.invoke(endpoint)
                if (restartOnChange) AppActions.restart(context)
            },
            optionLabel = { it.name },
        )
    }

    public companion object {
        public const val DEFAULT_KEY: String = "default"

        public fun current(
            context: Context,
            endpoints: List<Endpoint>,
            default: Endpoint = endpoints.first(),
            key: String = DEFAULT_KEY,
        ): Endpoint {
            val url = DeckSettings.of(context).getString(prefKey(key), null)
            return endpoints.firstOrNull { it.url == url } ?: default
        }

        // A restart kills the process right away, so only then must the write hit disk before returning.
        private fun save(context: Context, key: String, endpoint: Endpoint, synchronous: Boolean) {
            DeckSettings.of(context).edit(commit = synchronous) { putString(prefKey(key), endpoint.url) }
        }

        private fun prefKey(key: String) = if (key == DEFAULT_KEY) "endpoint.url" else "endpoint.url_$key"
    }
}
