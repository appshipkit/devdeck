package io.devdeck.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckState
import io.devdeck.internal.Json
import io.devdeck.internal.JsonVisualTransformation
import io.devdeck.internal.copyToClipboard
import io.devdeck.internal.readClipboardText
import io.devdeck.internal.rememberJsonColors

@Composable
public fun JsonEditor(value: String, onValueCommit: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawerState = LocalDeckState.current
    var draft by rememberSaveable { mutableStateOf(Json.prettyOrNull(value) ?: value) }
    val error = remember(draft) { Json.error(draft) }
    val colors = rememberJsonColors()
    val transformation = remember(colors) { JsonVisualTransformation(colors) }

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { Json.prettyOrNull(draft)?.let { draft = it } }, enabled = error == null) { Text("Format") }
            TextButton(onClick = {
                runCatching { Json.minify(draft) }.onSuccess { draft = it }
            }, enabled = error == null) { Text("Minify") }
            TextButton(onClick = { context.copyToClipboard("JSON", draft) }) { Text("Copy") }
            TextButton(onClick = { context.readClipboardText()?.let { draft = it } }) { Text("Paste") }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            visualTransformation = transformation,
            isError = error != null,
            supportingText = { Text(error ?: "Valid JSON · ${draft.length} chars") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            minLines = 14,
            modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            OutlinedButton(onClick = { draft = Json.prettyOrNull(value) ?: value }) { Text("Revert") }
            Button(
                onClick = {
                    onValueCommit(Json.minify(draft))
                    drawerState?.navigateBack()
                },
                enabled = error == null,
            ) { Text("Apply") }
        }
    }
}
