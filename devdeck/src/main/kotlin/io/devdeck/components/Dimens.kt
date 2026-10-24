package io.devdeck.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal val HorizontalPadding = 16.dp
internal val RowMinHeight = 52.dp
internal val RowPadding = Modifier.heightIn(min = RowMinHeight).padding(horizontal = HorizontalPadding, vertical = 12.dp)
