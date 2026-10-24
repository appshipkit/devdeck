package io.devdeck.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
public fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = HorizontalPadding, vertical = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
