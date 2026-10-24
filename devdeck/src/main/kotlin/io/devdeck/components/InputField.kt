package io.devdeck.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun InputField(
    title: String,
    value: String,
    onValueCommit: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    keyboardType: KeyboardType,
    singleLine: Boolean,
    commitOnEveryChange: Boolean = false,
    monospace: Boolean = false,
    validate: ((String) -> String?)? = null,
    format: ((String) -> String?)? = null,
) {
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    val error = validate?.invoke(draft)
    val focusManager = LocalFocusManager.current
    val commit = {
        if (error == null && draft != value) onValueCommit(draft)
        focusManager.clearFocus()
    }
    Column(modifier.fillMaxWidth().padding(horizontal = HorizontalPadding, vertical = 6.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it
                if (commitOnEveryChange && validate?.invoke(it) == null) onValueCommit(it)
            },
            label = { Text(title) },
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            maxLines = if (singleLine) 1 else 10,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            textStyle = if (monospace) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        val formatted = format?.invoke(draft)
        val canFormat = formatted != null && formatted != draft
        if (canFormat || (!commitOnEveryChange && draft != value)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (canFormat) TextButton(onClick = { draft = formatted!! }) { Text("Format") }
                if (!commitOnEveryChange && draft != value) {
                    TextButton(onClick = { draft = value }) { Text("Revert") }
                    TextButton(onClick = commit, enabled = error == null) { Text("Apply") }
                }
            }
        }
    }
}
