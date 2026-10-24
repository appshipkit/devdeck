package io.devdeck.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
public fun <T> SingleSelectItem(
    title: String,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    collapsible: Boolean = true,
    optionLabel: (T) -> String = { it.toString() },
) {
    val optionsContent: @Composable ColumnScope.() -> Unit = {
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option == selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { if (option != selected) onSelected(option) },
                    )
                    .heightIn(min = 48.dp)
                    .padding(horizontal = HorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RadioButton(selected = option == selected, onClick = null, enabled = enabled)
                Text(optionLabel(option), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (collapsible) {
        ExpandableItem(
            title = title,
            subtitle = selected?.let(optionLabel),
            modifier = modifier,
            content = optionsContent,
        )
    } else {
        Column(modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = 12.dp),
            )
            optionsContent()
        }
    }
}
