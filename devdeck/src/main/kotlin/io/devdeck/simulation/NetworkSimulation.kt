package io.devdeck.simulation

import androidx.compose.runtime.Immutable

public enum class NetworkCondition(public val label: String) {
    Normal("Normal"),
    Slow("Slow"),
    Failing("Failing"),
}

public enum class NetworkFailure(public val label: String) {
    NoConnection("No connection"),
    Timeout("Timeout"),
    ServerError("Server error 503"),
}

@Immutable
public data class NetworkSimulation(
    val condition: NetworkCondition = NetworkCondition.Normal,
    val delayMillis: Long = 3_000L,
    val bytesPerSecond: Long = 50L * 1024,
    val failure: NetworkFailure = NetworkFailure.NoConnection,
    val failureRatePercent: Int = 100,
    val urlFilter: String = "",
) {
    val isActive: Boolean get() = condition != NetworkCondition.Normal

    val summary: String
        get() = when (condition) {
            NetworkCondition.Normal -> "Normal network"
            NetworkCondition.Slow -> "Slow network"
            NetworkCondition.Failing -> failure.label + if (failureRatePercent < 100) " · $failureRatePercent%" else ""
        } + if (isActive && urlFilter.isNotBlank()) " · $urlFilter" else ""
}

public sealed interface NetworkEffect {
    public val label: String

    public data class Slow(val delayMillis: Long, val bytesPerSecond: Long) : NetworkEffect {
        override val label: String get() = "Slow"
    }

    public data class Fail(val failure: NetworkFailure) : NetworkEffect {
        override val label: String get() = failure.label
    }
}
