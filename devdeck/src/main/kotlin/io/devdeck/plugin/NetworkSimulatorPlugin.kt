package io.devdeck.plugin

import android.content.Context
import androidx.compose.runtime.Composable
import io.devdeck.simulation.NetworkSimulator
import io.devdeck.simulation.NetworkSimulatorSettings

public class NetworkSimulatorPlugin(override val section: String? = "Simulate 🧪") : DeckPlugin {

    override val id: String = "network_simulator"

    override fun onAttach(context: Context) {
        NetworkSimulator.init(context)
    }

    @Composable
    override fun Content() {
        NetworkSimulatorSettings()
    }
}
