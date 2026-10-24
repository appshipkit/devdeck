package io.devdeck.simulation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.BulletItem
import io.devdeck.components.ButtonItem
import io.devdeck.components.ExpandableItem
import io.devdeck.components.SwitchItem
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
public fun MemorySimulatorSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        ExpandableItem("Send memory warning") {
            MemoryWarning.entries.forEach { warning ->
                ButtonItem(title = warning.label, onClick = {
                    MemorySimulator.send(warning)
                    Toast.makeText(context, "Sent: ${warning.label}", Toast.LENGTH_SHORT).show()
                })
            }
        }
        ExpandableItem("Fill memory") { MemoryBalloon() }
        SwitchItem(
            title = "Kill app when backgrounded",
            subtitle = "Reopen it from recents to test state restore after process death",
            checked = MemorySimulator.killOnBackground,
            onCheckedChange = { MemorySimulator.killOnBackground = it },
        )
    }
}

@Composable
private fun MemoryBalloon() {
    val heap by produceState(heapUsage(), MemorySimulator.heldBytes) {
        while (true) {
            value = heapUsage()
            delay(1_000)
        }
    }
    BulletItem("Held by DevDeck", mb(MemorySimulator.heldBytes))
    BulletItem("Java heap", "${mb(heap.first)} of ${mb(heap.second)}")
    ButtonItem(title = "Hold 16 MB more", onClick = { MemorySimulator.allocate(16) })
    ButtonItem(title = "Hold 64 MB more", onClick = { MemorySimulator.allocate(64) })
    ButtonItem(title = "Fill heap to 90%", onClick = { MemorySimulator.fillHeap(90) })
    ButtonItem(title = "Release memory", enabled = MemorySimulator.heldBytes > 0, onClick = MemorySimulator::release)
}

private fun heapUsage(): Pair<Long, Long> {
    val runtime = Runtime.getRuntime()
    return (runtime.totalMemory() - runtime.freeMemory()) to runtime.maxMemory()
}

internal fun mb(bytes: Long): String = String.format(Locale.US, "%.0f MB", bytes / (1024.0 * 1024.0))
