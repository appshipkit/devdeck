package io.devdeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@Composable
internal fun SectionBadge(title: String, emoji: String?, modifier: Modifier = Modifier) {
    val color = BadgeColors[title.hashCode().absoluteValue % BadgeColors.size]
    Box(
        modifier.size(36.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji ?: title.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

internal fun splitSectionTitle(title: String): Pair<String?, String> {
    val trimmed = title.trim()
    val last = trimmed.substringAfterLast(' ', "")
    if (last.isEmoji()) return last to trimmed.substringBeforeLast(' ').trim()
    val first = trimmed.substringBefore(' ', "")
    if (first.isEmoji()) return first to trimmed.substringAfter(' ').trim()
    return null to trimmed
}

private fun String.isEmoji(): Boolean =
    isNotEmpty() && (any { it.isSurrogate() || it == EMOJI_PRESENTATION } || none { it.isLetterOrDigit() })

private const val EMOJI_PRESENTATION = '\uFE0F'

private val BadgeColors = listOf(
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFF4511E),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFFFFB300),
    Color(0xFFE53935),
    Color(0xFF3949AB),
)
