package io.devdeck.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
public fun ConfirmButtonItem(
    title: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    confirmTitle: String = "Tap again to confirm",
    destructive: Boolean = true,
) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(CONFIRM_WINDOW_MILLIS)
            armed = false
        }
    }
    ButtonItem(
        title = if (armed) confirmTitle else title,
        subtitle = subtitle,
        destructive = destructive,
        modifier = modifier,
        onClick = {
            if (armed) {
                armed = false
                onConfirm()
            } else {
                armed = true
            }
        },
    )
}

private const val CONFIRM_WINDOW_MILLIS = 3_000L
