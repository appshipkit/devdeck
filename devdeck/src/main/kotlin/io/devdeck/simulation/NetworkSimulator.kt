package io.devdeck.simulation

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import io.devdeck.internal.DeckSettings
import kotlin.random.Random

public object NetworkSimulator {

    private var state by mutableStateOf(NetworkSimulation())

    private var prefs: SharedPreferences? = null

    internal var random: Random = Random.Default

    public var simulation: NetworkSimulation
        get() = state
        set(value) {
            state = value.copy(
                delayMillis = value.delayMillis.coerceAtLeast(0),
                bytesPerSecond = value.bytesPerSecond.coerceAtLeast(0),
                failureRatePercent = value.failureRatePercent.coerceIn(1, 100),
                urlFilter = value.urlFilter.trim(),
            )
            persist(state)
        }

    public val isActive: Boolean get() = state.isActive

    public fun init(context: Context) {
        if (prefs != null) return
        val preferences = DeckSettings.of(context)
        prefs = preferences
        state = preferences.read()
    }

    public fun reset() {
        simulation = NetworkSimulation()
    }

    public fun effectFor(url: String): NetworkEffect? {
        val current = state
        if (!current.isActive) return null
        if (current.urlFilter.isNotEmpty() && !url.contains(current.urlFilter, ignoreCase = true)) return null
        return when (current.condition) {
            NetworkCondition.Normal -> null

            NetworkCondition.Slow -> NetworkEffect.Slow(current.delayMillis, current.bytesPerSecond)

            NetworkCondition.Failing ->
                if (random.nextInt(100) < current.failureRatePercent) NetworkEffect.Fail(current.failure) else null
        }
    }

    private fun persist(value: NetworkSimulation) {
        prefs?.edit {
            putString(KEY_CONDITION, value.condition.name)
            putLong(KEY_DELAY, value.delayMillis)
            putLong(KEY_BANDWIDTH, value.bytesPerSecond)
            putString(KEY_FAILURE, value.failure.name)
            putInt(KEY_FAILURE_RATE, value.failureRatePercent)
            putString(KEY_URL_FILTER, value.urlFilter)
        }
    }

    private fun SharedPreferences.read(): NetworkSimulation {
        val defaults = NetworkSimulation()
        return NetworkSimulation(
            condition = NetworkCondition.entries.firstOrNull { it.name == getString(KEY_CONDITION, null) } ?: defaults.condition,
            delayMillis = getLong(KEY_DELAY, defaults.delayMillis),
            bytesPerSecond = getLong(KEY_BANDWIDTH, defaults.bytesPerSecond),
            failure = NetworkFailure.entries.firstOrNull { it.name == getString(KEY_FAILURE, null) } ?: defaults.failure,
            failureRatePercent = getInt(KEY_FAILURE_RATE, defaults.failureRatePercent),
            urlFilter = getString(KEY_URL_FILTER, null) ?: defaults.urlFilter,
        )
    }

    private const val KEY_CONDITION = "network.condition"
    private const val KEY_DELAY = "network.delay_millis"
    private const val KEY_BANDWIDTH = "network.bytes_per_second"
    private const val KEY_FAILURE = "network.failure"
    private const val KEY_FAILURE_RATE = "network.failure_rate_percent"
    private const val KEY_URL_FILTER = "network.url_filter"
}
