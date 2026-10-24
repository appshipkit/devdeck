package io.devdeck

import android.app.Activity
import androidx.compose.runtime.Composable
import io.devdeck.plugin.DeckPlugin
import io.devdeck.plugin.DeckPlugins

public data class DeckConfig(
    val plugins: List<DeckPlugin> = DeckPlugins.defaults(),
    val side: DeckSide = DeckSide.End,
    val gestures: DeckGestures = DeckGestures(),
    val trigger: DeckTrigger = DeckTrigger.FloatingButton(),
    val style: DeckStyle = DeckStyle(),
    val includeActivity: (Activity) -> Boolean = { true },
    val excludedActivityPrefixes: Set<String> = DeckExclusions.PAYMENT_SDKS,
    val content: (@Composable DeckScope.() -> Unit)? = null,
)
