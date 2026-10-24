package io.devdeck.internal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

@Immutable
internal data class JsonColors(val key: Color, val string: Color, val number: Color, val literal: Color, val punctuation: Color)

@Composable
internal fun rememberJsonColors(): JsonColors = if (isSystemInDarkTheme()) {
    JsonColors(
        key = Color(0xFFCE93D8),
        string = Color(0xFFA5D6A7),
        number = Color(0xFF90CAF9),
        literal = Color(0xFFFFCC80),
        punctuation = MaterialTheme.colorScheme.onSurfaceVariant,
    )
} else {
    JsonColors(
        key = Color(0xFF7B1FA2),
        string = Color(0xFF2E7D32),
        number = Color(0xFF1565C0),
        literal = Color(0xFFE65100),
        punctuation = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun highlightJson(text: String, colors: JsonColors): AnnotatedString = buildAnnotatedString {
    append(text)
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            char == '"' -> {
                val end = stringEnd(text, index)
                val next = text.indexOfFirstNonSpace(end)
                val color = if (next < text.length && text[next] == ':') colors.key else colors.string
                addStyle(SpanStyle(color = color), index, end)
                index = end
            }

            char == '-' || char.isDigit() -> {
                var end = index + 1
                while (end < text.length && (text[end].isDigit() || text[end] in ".eE+-")) end++
                addStyle(SpanStyle(color = colors.number), index, end)
                index = end
            }

            text.startsWith("true", index) || text.startsWith("null", index) -> {
                addStyle(SpanStyle(color = colors.literal), index, index + 4)
                index += 4
            }

            text.startsWith("false", index) -> {
                addStyle(SpanStyle(color = colors.literal), index, index + 5)
                index += 5
            }

            char in "{}[]:," -> {
                addStyle(SpanStyle(color = colors.punctuation), index, index + 1)
                index++
            }

            else -> index++
        }
    }
}

internal class JsonVisualTransformation(private val colors: JsonColors) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(highlightJson(text.text, colors), OffsetMapping.Identity)

    override fun equals(other: Any?): Boolean = other is JsonVisualTransformation && other.colors == colors

    override fun hashCode(): Int = colors.hashCode()
}

private fun stringEnd(text: String, start: Int): Int {
    var index = start + 1
    while (index < text.length) {
        when (text[index]) {
            '\\' -> index += 2
            '"' -> return index + 1
            else -> index++
        }
    }
    return text.length
}

private fun String.indexOfFirstNonSpace(from: Int): Int {
    var index = from
    while (index < length && this[index].isWhitespace()) index++
    return index
}
