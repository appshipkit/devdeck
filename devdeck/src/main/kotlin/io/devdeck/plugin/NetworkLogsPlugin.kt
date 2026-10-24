package io.devdeck.plugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.devdeck.components.PageLinkItem
import io.devdeck.network.NetworkHighlight
import io.devdeck.network.NetworkInspector
import io.devdeck.network.NetworkLogger
import io.devdeck.network.NetworkView
import io.devdeck.network.groupKey

public class NetworkLogsPlugin(
    private val highlights: List<NetworkHighlight> = listOf(NetworkHighlight.graphQl()),
    private val title: String = "Network activity",
    private val initialView: NetworkView = NetworkView.Timeline,
    override val section: String? = "Logs 📚",
) : DeckPlugin {

    override val id: String = "network_logs"

    @Composable
    override fun Content() {
        val calls by NetworkLogger.calls.collectAsState()
        val failures = calls.count { it.isFailure }
        val endpoints = calls.distinctBy { it.groupKey }.size
        PageLinkItem(
            title = title,
            subtitle = if (calls.isEmpty()) "No requests yet" else "${calls.size} requests · $endpoints endpoints · $failures failed",
        ) { NetworkInspector(highlights = highlights, initialView = initialView) }
    }
}
