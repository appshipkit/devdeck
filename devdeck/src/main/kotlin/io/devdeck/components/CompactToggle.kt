package io.devdeck.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
public fun CompactToggle(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val fraction by animateFloatAsState(if (checked) 1f else 0f, label = "toggle")
    val trackColor by animateColorAsState(
        if (checked) colors.primary.copy(alpha = 0.5f) else colors.onSurface.copy(alpha = 0.3f),
        label = "track",
    )
    val thumbColor by animateColorAsState(if (checked) colors.primary else Color(0xFFF1F1F1), label = "thumb")
    Box(modifier.size(width = 36.dp, height = 22.dp).alpha(if (enabled) 1f else 0.5f), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .padding(horizontal = 1.dp)
                .size(width = 34.dp, height = 14.dp)
                .background(trackColor, RoundedCornerShape(7.dp)),
        )
        Box(
            Modifier
                .offset { IntOffset((14.dp.toPx() * fraction).roundToInt(), 0) }
                .padding(1.dp)
                .size(20.dp)
                .shadow(2.dp, CircleShape)
                .background(thumbColor, CircleShape),
        )
    }
}
