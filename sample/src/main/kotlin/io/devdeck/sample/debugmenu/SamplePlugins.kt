package io.devdeck.sample.debugmenu

import android.content.Context
import androidx.compose.ui.graphics.Color
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
import io.devdeck.timber.TimberLogsPlugin

fun samplePlugins(context: Context, flagStore: FeatureFlagStore): List<DeckPlugin> = listOf(
    SampleEndpoints.plugin,
    FeatureFlagsPlugin(SampleFlags.all, flagStore),
    NetworkSimulatorPlugin(),
    MemorySimulatorPlugin(),
    NetworkLogsPlugin(highlights = networkHighlights),
    LogcatPlugin(),
    TimberLogsPlugin(),
    LogsPlugin(tags = listOf("General", "Analytics"), lifecycleLogs = true),
    BreadcrumbsPlugin(),
    CrashPlugin(testCrashButton = true),
    AppActionsPlugin(deepLinkSuggestions = listOf("devdeck://sample/legacy")),
    UpdatesPlugin(source = SampleUpdateSource, notify = true),
    UiToolsPlugin(),
    AppearancePlugin(languageTags = listOf("en", "es", "fr", "hi")),
    PerformancePlugin(),
    StoragePlugin(),
    DevicePlugin(),
    tapCounterPlugin,
    controlsDemoPlugin,
    InfoPlugin(buildInfo = { mapOf("Endpoint" to SampleEndpoints.current(context).url) }),
)

private val networkHighlights = listOf(
    NetworkHighlight.graphQl(),
    NetworkHighlight.operation("Countries", Color(0xFF00897B), "Countries"),
    NetworkHighlight.urlContains("Orders", Color(0xFFFB8C00), "/orders"),
)
