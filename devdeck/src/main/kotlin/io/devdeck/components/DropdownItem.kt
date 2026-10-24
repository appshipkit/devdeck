package io.devdeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckColors
import io.devdeck.internal.DeckIcons

@Composable
public fun <T> DropdownItem(
    title: String,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    placeholder: String = "Select",
    optionLabel: (T) -> String = { it.toString() },
) {
    var open by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.DropdownList) { open = true }
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleAndSubtitle(title, subtitle, enabled, Modifier.weight(1f))
        Box {
            Row(
                Modifier
                    .alpha(if (enabled) 1f else 0.4f)
                    .background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected?.let(optionLabel) ?: placeholder,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp),
                )
                Icon(
                    DeckIcons.ExpandMore,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp).rotate(if (open) 180f else 0f),
                )
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = LocalDeckColors.current?.card ?: colors.surface,
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                optionLabel(option),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    DeckIcons.Check,
                                    contentDescription = "Selected",
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            open = false
                            if (!isSelected) onSelected(option)
                        },
                    )
                }
            }
        }
    }
}
