package io.devdeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.devdeck.internal.DeckIcons
import kotlinx.coroutines.delay

/** A delete icon button that asks for a second tap within a few seconds before running [onConfirm]. */
@Composable
internal fun ConfirmDeleteButton(
    contentDescription: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    confirmText: String = "Tap again to clear",
) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed, enabled) {
        if (armed && enabled) delay(CONFIRM_WINDOW_MILLIS)
        armed = false
    }
    val error = MaterialTheme.colorScheme.error
    Box(
        modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = if (armed) confirmText else null) {
                if (armed) {
                    armed = false
                    onConfirm()
                } else {
                    armed = true
                }
            }
            .semantics { if (!armed) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (armed) {
            Text(
                confirmText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = error,
                maxLines = 1,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .background(error.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        } else {
            Icon(DeckIcons.Delete, contentDescription = null, tint = error.copy(alpha = if (enabled) 1f else 0.38f))
        }
    }
}

private const val CONFIRM_WINDOW_MILLIS = 3_000L
