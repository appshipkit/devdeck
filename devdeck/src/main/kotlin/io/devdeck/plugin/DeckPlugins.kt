package io.devdeck.plugin

public object DeckPlugins {
    public fun defaults(): List<DeckPlugin> = listOf(
        NetworkSimulatorPlugin(),
        MemorySimulatorPlugin(),
        NetworkLogsPlugin(),
        LogcatPlugin(),
        LogsPlugin(),
        BreadcrumbsPlugin(),
        CrashPlugin(),
        AppActionsPlugin(),
        UiToolsPlugin(),
        AppearancePlugin(),
        PerformancePlugin(),
        StoragePlugin(),
        DevicePlugin(),
        InfoPlugin(),
    )
}
