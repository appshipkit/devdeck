package io.devdeck.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.devdeck.internal.DeckIcons

@Composable
public fun DeckRadio(selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (selected) colors.primary else Color.Transparent, label = "radioFill")
    val dot by animateDpAsState(if (selected) 8.dp else 0.dp, label = "radioDot")
    Box(
        modifier
            .size(22.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .background(fill, CircleShape)
            .border(2.dp, if (selected) colors.primary else colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(dot).background(colors.onPrimary, CircleShape))
    }
}

@Composable
public fun DeckCheckbox(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(6.dp)
    val fill by animateColorAsState(if (checked) colors.primary else Color.Transparent, label = "checkboxFill")
    Box(
        modifier
            .size(22.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .background(fill, shape)
            .border(2.dp, if (checked) colors.primary else colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(DeckIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
    }
}
