package io.devdeck.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun PerformanceOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val stats by rememberFrameStats()
    val memory by produceState(PerformanceMonitor.memory(context)) {
        while (true) {
            delay(1_000)
            value = PerformanceMonitor.memory(context)
        }
    }
    val color = when {
        stats.fps >= 55 -> Color(0xFF2E7D32)
        stats.fps >= 40 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }
    Box(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Text(
            text = "${stats.fps} fps · jank ${stats.jankyFrames} · ${memory.heapUsedBytes / MB}/${memory.heapMaxBytes / MB} MB",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .background(color.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private const val MB = 1024 * 1024
