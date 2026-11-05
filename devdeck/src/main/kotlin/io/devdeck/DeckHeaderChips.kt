package io.devdeck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

/** A chip in the drawer header, under the title. [color] tints it; null draws a neutral outlined chip. */
@Immutable
public class DeckHeaderChip(public val text: String, public val color: Color? = null, public val onClick: (() -> Unit)? = null)

/**
 * Chips the app adds to the drawer header, next to DevDeck's own (recording, new version, simulations). Each chip is
 * a composable that returns null while it has nothing to show, so it can follow Compose state:
 *
 * ```
 * DeckHeaderChips.add("environment") { if (isStaging) DeckHeaderChip("Staging", Color(0xFFF57C00)) else null }
 * ```
 */
public object DeckHeaderChips {

    private val entries = mutableStateListOf<Pair<String, @Composable () -> DeckHeaderChip?>>()

    /** Adds a chip, or replaces the one already added under [key]. */
    public fun add(key: String, chip: @Composable () -> DeckHeaderChip?) {
        val index = entries.indexOfFirst { it.first == key }
        if (index >= 0) entries[index] = key to chip else entries += key to chip
    }

    public fun remove(key: String) {
        entries.removeAll { it.first == key }
    }

    @Composable
    internal fun current(): List<DeckHeaderChip> = entries.toList().mapNotNull { (_, chip) -> chip() }
}
