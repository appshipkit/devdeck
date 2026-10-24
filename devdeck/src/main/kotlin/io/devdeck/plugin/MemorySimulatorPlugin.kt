package io.devdeck.plugin

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import io.devdeck.simulation.MemorySimulator
import io.devdeck.simulation.MemorySimulatorSettings

public class MemorySimulatorPlugin(override val section: String? = "Simulate 🧪") : DeckPlugin {

    override val id: String = "memory_simulator"

    override fun onAttach(context: Context) {
        (context.applicationContext as? Application)?.let(MemorySimulator::init)
    }

    @Composable
    override fun Content() {
        MemorySimulatorSettings()
    }
}
