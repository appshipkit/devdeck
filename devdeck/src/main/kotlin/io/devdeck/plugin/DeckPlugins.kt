package io.devdeck.plugin

public object DeckPlugins {
    public fun defaults(): List<DeckPlugin> = listOf(
        NetworkSimulatorPlugin(),
        MemorySimulatorPlugin(),
        NetworkLogsPlugin(),
        LogcatPlugin(),
        LogsPlugin(),
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
