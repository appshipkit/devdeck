package io.devdeck.plugin

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.devdeck.breadcrumbs.BreadcrumbCollector
import io.devdeck.breadcrumbs.BreadcrumbList
import io.devdeck.breadcrumbs.Breadcrumbs
import io.devdeck.components.PageLinkItem

public class BreadcrumbsPlugin(
    private val lifecycle: Boolean = true,
    private val taps: Boolean = true,
    private val network: Boolean = true,
    private val title: String = "Breadcrumbs",
    override val section: String? = "Logs 📚",
) : DeckPlugin {

    override val id: String = "breadcrumbs"

    override fun onAttach(context: Context) {
        Breadcrumbs.captureNetwork = network
        (context.applicationContext as? Application)?.let { BreadcrumbCollector.install(it, lifecycle, taps) }
    }

    @Composable
    override fun Content() {
        val entries by Breadcrumbs.entries.collectAsState()
        PageLinkItem(title = title, subtitle = "${entries.size} steps · screens, taps, requests and app events") { BreadcrumbList() }
    }
}
