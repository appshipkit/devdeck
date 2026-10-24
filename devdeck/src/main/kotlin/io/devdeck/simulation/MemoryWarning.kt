package io.devdeck.simulation

public enum class MemoryWarning(public val label: String, public val level: Int?) {
    RunningModerate("Running moderate", 5),
    RunningLow("Running low", 10),
    RunningCritical("Running critical", 15),
    UiHidden("UI hidden", 20),
    Background("Background", 40),
    Moderate("Moderate", 60),
    Complete("Complete", 80),
    LowMemory("Low memory", null),
}

public val CommonMemoryWarnings: List<MemoryWarning> = listOf(
    MemoryWarning.UiHidden,
    MemoryWarning.Background,
    MemoryWarning.RunningLow,
    MemoryWarning.RunningCritical,
    MemoryWarning.LowMemory,
)
