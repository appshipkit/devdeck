package io.devdeck.simulation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun SimulationBadge(modifier: Modifier = Modifier) {
    val lines = buildList {
        val network = NetworkSimulator.simulation
        if (network.isActive) add(if (network.condition == NetworkCondition.Slow) "🐢 ${network.summary}" else "📵 ${network.summary}")
        if (MemorySimulator.heldBytes > 0) add("🧠 Holding ${mb(MemorySimulator.heldBytes)}")
        if (MemorySimulator.killOnBackground) add("💀 Kill when backgrounded")
    }
    if (lines.isEmpty()) return
    Box(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Text(
            text = lines.joinToString("\n"),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(BadgeColor.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// Dark enough for white text to pass contrast.
private val BadgeColor = Color(0xFFBF360C)
