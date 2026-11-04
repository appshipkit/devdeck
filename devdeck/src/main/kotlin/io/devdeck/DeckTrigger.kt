package io.devdeck

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
public sealed interface DeckTrigger {

    public data object None : DeckTrigger

    /**
     * A round button that opens the drawer. [icon] replaces the default bug icon with any composable, for example
     * `{ Icon(painterResource(R.drawable.ic_debug), contentDescription = "Open debug menu") }`.
     */
    public data class FloatingButton(
        val alignment: Alignment = Alignment.CenterEnd,
        val draggable: Boolean = true,
        val icon: (@Composable () -> Unit)? = null,
    ) : DeckTrigger {

        /** Shows [icon] (for example `Icons.Default.Build`) instead of the default bug icon. */
        public constructor(
            icon: ImageVector,
            alignment: Alignment = Alignment.CenterEnd,
            draggable: Boolean = true,
        ) : this(alignment, draggable, { Icon(icon, contentDescription = OPEN_DESCRIPTION) })
    }
}

internal const val OPEN_DESCRIPTION = "Open debug menu"
