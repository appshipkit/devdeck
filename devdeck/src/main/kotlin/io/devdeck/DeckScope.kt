package io.devdeck

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalDeckState: ProvidableCompositionLocal<DeckState?> = staticCompositionLocalOf { null }

@Stable
public interface DeckScope {
    public val drawerState: DeckState
}

internal class DeckScopeImpl(override val drawerState: DeckState) : DeckScope
