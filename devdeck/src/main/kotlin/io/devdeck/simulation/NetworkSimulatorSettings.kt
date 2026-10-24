package io.devdeck.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.devdeck.components.ButtonItem
import io.devdeck.components.DropdownItem
import io.devdeck.components.NumberInputItem
import io.devdeck.components.TextInputItem

@Composable
public fun NetworkSimulatorSettings(modifier: Modifier = Modifier, title: String = "Network") {
    val simulation = NetworkSimulator.simulation
    val update: (NetworkSimulation) -> Unit = { NetworkSimulator.simulation = it }
    Column(modifier.fillMaxWidth()) {
        DropdownItem(
            title = title,
            options = NetworkCondition.entries,
            selected = simulation.condition,
            onSelected = { update(simulation.copy(condition = it)) },
            optionLabel = { it.label },
        )
        AnimatedVisibility(simulation.condition == NetworkCondition.Slow) {
            Column {
                NumberInputItem(
                    title = "Extra delay per request (ms)",
                    value = simulation.delayMillis,
                    onValueCommit = { update(simulation.copy(delayMillis = it)) },
                    range = 0L..MAX_DELAY_MILLIS,
                )
                NumberInputItem(
                    title = "Download speed (KB/s, 0 = unlimited)",
                    value = simulation.bytesPerSecond / KB,
                    onValueCommit = { update(simulation.copy(bytesPerSecond = it * KB)) },
                    range = 0L..MAX_SPEED_KBPS,
                )
            }
        }
        AnimatedVisibility(simulation.condition == NetworkCondition.Failing) {
            Column {
                DropdownItem(
                    title = "Failure",
                    options = NetworkFailure.entries,
                    selected = simulation.failure,
                    onSelected = { update(simulation.copy(failure = it)) },
                    optionLabel = { it.label },
                )
                NumberInputItem(
                    title = "Failure rate (%)",
                    value = simulation.failureRatePercent.toLong(),
                    onValueCommit = { update(simulation.copy(failureRatePercent = it.toInt())) },
                    range = 1L..100L,
                )
            }
        }
        AnimatedVisibility(simulation.isActive) {
            Column {
                TextInputItem(
                    title = "Only URLs containing (empty = all)",
                    value = simulation.urlFilter,
                    onValueCommit = { update(simulation.copy(urlFilter = it)) },
                )
                ButtonItem(title = "Back to normal network", onClick = NetworkSimulator::reset)
            }
        }
    }
}

private const val KB = 1024L
private const val MAX_DELAY_MILLIS = 120_000L
private const val MAX_SPEED_KBPS = 100_000L
