package io.devdeck.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
public fun NumberInputItem(
    title: String,
    value: Double,
    onValueCommit: (Double) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    allowDecimals: Boolean = true,
    range: ClosedFloatingPointRange<Double>? = null,
    subtitle: String? = null,
) {
    val text = if (allowDecimals) value.toString().removeSuffix(".0") else value.toLong().toString()
    InputField(
        title = title,
        value = text,
        onValueCommit = { raw -> raw.trim().toDoubleOrNull()?.let(onValueCommit) },
        modifier = modifier,
        enabled = enabled,
        keyboardType = if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number,
        singleLine = true,
        validate = { raw ->
            val number = raw.trim().toDoubleOrNull()
            when {
                number == null -> "Not a valid number"
                !allowDecimals && number % 1.0 != 0.0 -> "Whole numbers only"
                range != null && number !in range -> "Must be between ${range.start} and ${range.endInclusive}"
                else -> null
            }
        },
        subtitle = subtitle,
    )
}

@Composable
public fun NumberInputItem(
    title: String,
    value: Long,
    onValueCommit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: LongRange? = null,
    subtitle: String? = null,
) {
    NumberInputItem(
        title = title,
        value = value.toDouble(),
        onValueCommit = { onValueCommit(it.toLong()) },
        modifier = modifier,
        enabled = enabled,
        allowDecimals = false,
        range = range?.let { it.first.toDouble()..it.last.toDouble() },
        subtitle = subtitle,
    )
}
