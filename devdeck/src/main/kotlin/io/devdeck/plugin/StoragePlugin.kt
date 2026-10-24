package io.devdeck.plugin

import androidx.compose.runtime.Composable
import io.devdeck.components.ExpandableItem
import io.devdeck.components.PageLinkItem
import io.devdeck.storage.SharedPreferencesBrowser
import io.devdeck.storage.StorageUsage

public class StoragePlugin(
    private val sharedPreferences: Boolean = true,
    private val usage: Boolean = true,
    private val includePreferences: (String) -> Boolean = { true },
    override val section: String? = "Storage 💾",
) : DeckPlugin {

    override val id: String = "storage"

    @Composable
    override fun Content() {
        if (sharedPreferences) {
            PageLinkItem("Shared preferences", subtitle = "View, edit and delete keys") {
                SharedPreferencesBrowser(include = includePreferences)
            }
        }
        if (usage) ExpandableItem("Storage usage") { StorageUsage() }
    }
}
