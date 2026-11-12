package io.devdeck.sample.debugmenu

import android.content.Context
import androidx.compose.ui.graphics.Color
import io.devdeck.actions.DeepLink
import io.devdeck.actions.DeepLinkInput
import io.devdeck.flags.FeatureFlagStore
import io.devdeck.network.NetworkHighlight
import io.devdeck.plugin.AppActionsPlugin
import io.devdeck.plugin.AppearancePlugin
import io.devdeck.plugin.BreadcrumbsPlugin
import io.devdeck.plugin.CrashPlugin
import io.devdeck.plugin.DeckPlugin
import io.devdeck.plugin.DevicePlugin
import io.devdeck.plugin.FeatureFlagsPlugin
import io.devdeck.plugin.InfoPlugin
import io.devdeck.plugin.LogcatPlugin
import io.devdeck.plugin.LogsPlugin
import io.devdeck.plugin.MemorySimulatorPlugin
import io.devdeck.plugin.NetworkLogsPlugin
import io.devdeck.plugin.NetworkSimulatorPlugin
import io.devdeck.plugin.PerformancePlugin
import io.devdeck.plugin.StoragePlugin
import io.devdeck.plugin.UiToolsPlugin
import io.devdeck.plugin.UpdatesPlugin
import io.devdeck.storage.PreferenceFileRules
import io.devdeck.timber.TimberLogsPlugin

fun samplePlugins(context: Context, flagStore: FeatureFlagStore): List<DeckPlugin> = listOf(
    SampleEndpoints.plugin,
    // Flags still in development can be seen but not changed from the drawer.
    FeatureFlagsPlugin(SampleFlags.all, flagStore, readOnlyCategories = setOf("growth")),
    NetworkSimulatorPlugin(),
    MemorySimulatorPlugin(),
    NetworkLogsPlugin(highlights = networkHighlights),
    LogcatPlugin(),
    TimberLogsPlugin(),
    LogsPlugin(tags = listOf("General", "Analytics"), lifecycleLogs = true),
    BreadcrumbsPlugin(),
    CrashPlugin(testCrashButton = true),
    AppActionsPlugin(savedDeepLinks = sampleDeepLinks, deepLinkBaseUrl = "devdeck://sample/"),
    UpdatesPlugin(source = SampleUpdateSource, notify = true),
    UiToolsPlugin(),
    AppearancePlugin(languageTags = listOf("en", "es", "fr", "hi")),
    PerformancePlugin(),
    // Keeps a sample-only cache with the library files and never lists the session file.
    StoragePlugin(preferenceRules = PreferenceFileRules(library = setOf("image_cache*"), hidden = setOf("secure_session"))),
    DevicePlugin(),
    tapCounterPlugin,
    controlsDemoPlugin,
    InfoPlugin(buildInfo = { mapOf("Endpoint" to SampleEndpoints.current(context).url) }),
)

// Testers pick these by name; the URIs stay hidden. Links without a scheme get the devdeck://sample/ base URL.
private val sampleDeepLinks = listOf(
    DeepLink("View-based screen", "legacy", group = "Screens", description = "Screen using DevDeck.install"),
    DeepLink(
        "View-based screen for an item",
        "legacy?item={item}&source={source}",
        group = "Screens",
        description = "Asks for an item ID first",
        inputs = listOf(
            DeepLinkInput("item", label = "Item ID", default = "42"),
            DeepLinkInput("source", label = "Source", required = false),
        ),
    ),
    DeepLink("Endless feed", "feed", group = "Screens"),
    DeepLink("Missing screen", "missing", group = "Error cases", description = "Nothing handles it, so a toast says so"),
)

private val networkHighlights = listOf(
    NetworkHighlight.graphQl(),
    NetworkHighlight.operation("Countries", Color(0xFF00897B), "Countries"),
    NetworkHighlight.urlContains("Orders", Color(0xFFFB8C00), "/orders"),
)
