package io.devdeck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment

@Immutable
public sealed interface DeckTrigger {

    public data object None : DeckTrigger

    public data class FloatingButton(
        val alignment: Alignment = Alignment.CenterEnd,
        val draggable: Boolean = true,
        val icon: (@Composable () -> Unit)? = null,
    ) : DeckTrigger
}
