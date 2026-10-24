package io.devdeck.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.devdeck.DeckSide

@Composable
fun DrawerControls(options: DrawerOptions, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Drawer controls", style = MaterialTheme.typography.titleMedium)
        ControlSwitch("Drawer enabled", options.enabled) { options.enabled = it }
        ControlSwitch("Two-finger swipe to open", options.twoFingerSwipe) { options.twoFingerSwipe = it }
        ControlSwitch("Ctrl + Shift + D shortcut", options.keyboardShortcut) { options.keyboardShortcut = it }
        ControlSwitch("Floating button", options.floatingButton) { options.floatingButton = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Side", modifier = Modifier.padding(end = 8.dp))
            DeckSide.entries.forEach { side ->
                FilterChip(selected = options.side == side, onClick = { options.side = side }, label = { Text(side.name) })
            }
        }
    }
}

@Composable
private fun ControlSwitch(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
