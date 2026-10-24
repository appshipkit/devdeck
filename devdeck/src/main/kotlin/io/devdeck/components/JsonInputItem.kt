package io.devdeck.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.internal.Json

@Composable
public fun JsonInputItem(
    title: String,
    value: String,
    onValueCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    previewLines: Int = 6,
) {
    val drawerState = LocalDeckState.current
    val error = remember(value) { Json.error(value) }
    Column(modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = error ?: "${value.length} chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (enabled && drawerState != null) {
                OutlinedButton(onClick = {
                    drawerState.navigateTo(title) { JsonEditor(value = value, onValueCommit = onValueCommit) }
                }) { Text("Edit") }
            }
        }
        CodeBlock(
            text = value,
            language = CodeLanguage.Json,
            collapsedLines = previewLines,
            copyable = false,
        )
    }
}
