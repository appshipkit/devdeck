package io.devdeck.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun KeylineOverlay(modifier: Modifier = Modifier) {
    val gridSize = UiTools.keylineGridSize
    val keylines = UiTools.keylines
    Canvas(modifier.fillMaxSize()) {
        val step = gridSize.toPx()
        if (step > 0f) {
            var x = 0f
            while (x <= size.width) {
                drawLine(GridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(GridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        val strokeWidth = 1.dp.toPx()
        keylines.forEach { keyline ->
            val offset = keyline.toPx()
            drawLine(KeylineColor, Offset(offset, 0f), Offset(offset, size.height), strokeWidth)
            drawLine(KeylineColor, Offset(size.width - offset, 0f), Offset(size.width - offset, size.height), strokeWidth)
        }
    }
}

private val GridColor = Color(0x33FF00FF)
private val KeylineColor = Color(0xAAFF1744)
