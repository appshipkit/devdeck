package io.devdeck.sample.debugmenu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.devdeck.components.ExpandableItem
import io.devdeck.plugin.DeckPlugin
import io.devdeck.plugin.deckPlugin

val tapCounterPlugin: DeckPlugin = deckPlugin(id = "tap_counter", section = "Custom 🧩") {
    ExpandableItem("Custom component") {
        var taps by remember { mutableIntStateOf(0) }
        Text(
            "Any composable works here. Tapped $taps times.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        OutlinedButton(onClick = { taps++ }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Tap me")
        }
    }
}
