package io.devdeck.sample.debugmenu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.devdeck.components.CheckboxItem
import io.devdeck.components.DropdownItem
import io.devdeck.components.ExpandableItem
import io.devdeck.components.SingleSelectItem
import io.devdeck.components.SwitchItem
import io.devdeck.plugin.DeckPlugin
import io.devdeck.plugin.deckPlugin

val controlsDemoPlugin: DeckPlugin = deckPlugin(id = "controls_demo", section = "Custom 🧩") {
    ExpandableItem("Controls") {
        var verbose by remember { mutableStateOf(true) }
        var mockPayments by remember { mutableStateOf(false) }
        var skipOnboarding by remember { mutableStateOf(true) }
        var region by remember { mutableStateOf("US") }
        var cache by remember { mutableStateOf("Memory") }
        SwitchItem(title = "Verbose logging", checked = verbose, onCheckedChange = { verbose = it })
        CheckboxItem(title = "Mock payments", subtitle = "Checkbox", checked = mockPayments, onCheckedChange = { mockPayments = it })
        CheckboxItem(title = "Skip onboarding", checked = skipOnboarding, onCheckedChange = { skipOnboarding = it })
        DropdownItem(
            title = "Region",
            subtitle = "Dropdown",
            options = listOf("US", "UK", "EU", "Japan", "India"),
            selected = region,
            onSelected = { region = it },
        )
        SingleSelectItem(
            title = "Image cache",
            options = listOf("Memory", "Disk", "None"),
            selected = cache,
            onSelected = { cache = it },
            collapsible = false,
        )
    }
}
