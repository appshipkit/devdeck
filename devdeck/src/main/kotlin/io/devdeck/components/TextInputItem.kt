package io.devdeck.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
public fun TextInputItem(
    title: String,
    value: String,
    onValueCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    commitOnEveryChange: Boolean = false,
    singleLine: Boolean = true,
    validate: ((String) -> String?)? = null,
    subtitle: String? = null,
) {
    InputField(
        title = title,
        value = value,
        onValueCommit = onValueCommit,
        modifier = modifier,
        enabled = enabled,
        keyboardType = keyboardType,
        commitOnEveryChange = commitOnEveryChange,
        singleLine = singleLine,
        validate = validate,
        subtitle = subtitle,
    )
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
public fun TextInputItem(
    title: String,
    value: String,
    onValueCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    commitOnEveryChange: Boolean = false,
    singleLine: Boolean = true,
    validate: ((String) -> String?)? = null,
) {
    TextInputItem(title, value, onValueCommit, modifier, enabled, keyboardType, commitOnEveryChange, singleLine, validate, subtitle = null)
}
