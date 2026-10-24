package io.devdeck.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.devdeck.internal.DeckIcons
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
    onDoubleClick: (() -> Unit)? = null,
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
    val shape = RoundedCornerShape(10.dp)
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    val title = label ?: if (language == CodeLanguage.Json) "JSON" else null
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
    val hasDoubleClick = onDoubleClick != null

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(background, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        if (title != null || canExpand || copyable) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(start = 12.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (canExpand) {
                    ToolbarIcon(
                        icon = if (expanded) DeckIcons.UnfoldLess else DeckIcons.UnfoldMore,
                        description = if (expanded) "Collapse" else "Expand",
                        onClick = { expanded = !expanded },
                    )
                }
                if (copyable) {
                    ToolbarIcon(DeckIcons.ContentCopy, "Copy", onClick = { context.copyToClipboard(label ?: "Text", display) })
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .then(
                    if (canExpand || hasDoubleClick) {
                        Modifier.pointerInput(canExpand, hasDoubleClick) {
                            detectTapGestures(
                                onTap = if (canExpand) ({ expanded = !expanded }) else null,
                                onDoubleTap = if (hasDoubleClick) ({ currentOnDoubleClick?.invoke() }) else null,
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Text(
                text = highlighted,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = if (expanded || !canExpand) Int.MAX_VALUE else collapsedLines,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            )
            if (canExpand && !expanded) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, background)), shape),
                )
            }
        }
    }
}

@Composable
private fun ToolbarIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

private const val MAX_HIGHLIGHT_CHARS = 200_000
