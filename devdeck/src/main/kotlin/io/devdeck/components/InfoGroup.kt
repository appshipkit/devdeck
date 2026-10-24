package io.devdeck.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

@Composable
public fun InfoGroup(modifier: Modifier = Modifier, dividerThreshold: Int = DEFAULT_DIVIDER_THRESHOLD, content: @Composable () -> Unit) {
    val color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val dividers = remember { DividerPositions() }
    Layout(
        content = content,
        modifier = modifier.drawWithContent {
            drawContent()
            val inset = HorizontalPadding.toPx()
            val stroke = 1.dp.toPx()
            dividers.ys.forEach { y -> drawLine(color, Offset(inset, y), Offset(size.width - inset, y), stroke) }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val height = placeables.sumOf { it.height }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val visible = placeables.filter { it.height > 0 }
        dividers.ys = if (visible.size >= dividerThreshold) {
            var y = 0
            placeables.dropLast(1).mapNotNull { placeable ->
                y += placeable.height
                if (placeable.height > 0) y.toFloat() else null
            }
        } else {
            emptyList()
        }
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.place(0, y)
                y += placeable.height
            }
        }
    }
}

private class DividerPositions {
    var ys: List<Float> = emptyList()
}

private const val DEFAULT_DIVIDER_THRESHOLD = 5
