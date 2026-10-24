package io.devdeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
public fun ActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val accent = if (color.isSpecified) color else MaterialTheme.colorScheme.primary
    Text(
        text = text,
        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = accent,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = if (compact) 28.dp else 36.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = if (compact) 10.dp else 14.dp),
    )
}

@Composable
public fun ActionPillRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier.fillMaxWidth().padding(horizontal = HorizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
