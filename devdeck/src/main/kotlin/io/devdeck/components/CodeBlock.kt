package io.devdeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.devdeck.internal.Json
import io.devdeck.internal.copyToClipboard
import io.devdeck.internal.highlightJson
import io.devdeck.internal.rememberJsonColors

public enum class CodeLanguage { Json, Plain }

@Composable
public fun CodeBlock(
    text: String,
    modifier: Modifier = Modifier,
    language: CodeLanguage = CodeLanguage.Plain,
    label: String? = null,
    collapsedLines: Int = 12,
    copyable: Boolean = true,
    prettyPrint: Boolean = true,
) {
    val context = LocalContext.current
    val colors = rememberJsonColors()
    val display = remember(text, language, prettyPrint) {
        if (language == CodeLanguage.Json && prettyPrint) Json.prettyOrNull(text) ?: text else text
    }
    val highlighted = remember(display, language, colors) {
        if (language == CodeLanguage.Json &&
            display.length <= MAX_HIGHLIGHT_CHARS
        ) {
            highlightJson(display, colors)
        } else {
            AnnotatedString(display)
        }
    }
    val lineCount = remember(display) { display.count { it == '\n' } + 1 }
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    val canExpand = lineCount > collapsedLines

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        if (label != null || copyable || (canExpand && expanded)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (canExpand && expanded) {
                    TextButton(onClick = { expanded = false }) { Text("Collapse") }
                }
                if (copyable) TextButton(onClick = { context.copyToClipboard(label ?: "Text", display) }) { Text("Copy") }
            }
        }
        Text(
            text = highlighted,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = if (expanded || !canExpand) Int.MAX_VALUE else collapsedLines,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
        if (canExpand && !expanded) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { expanded = true }) { Text("Show all $lineCount lines") }
            }
        }
    }
}

private const val MAX_HIGHLIGHT_CHARS = 200_000
