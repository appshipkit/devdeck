package io.devdeck.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal val HorizontalPadding = 16.dp

// Material's 48dp minimum touch target; rows grow with a subtitle.
internal val RowMinHeight = 48.dp
internal val RowPadding = Modifier.heightIn(min = RowMinHeight).padding(horizontal = HorizontalPadding, vertical = 8.dp)
