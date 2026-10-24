package io.devdeck.components

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

internal class RevealOnExpand(private val requester: BringIntoViewRequester) {
    var requests by mutableIntStateOf(0)
        private set

    val modifier: Modifier = Modifier.bringIntoViewRequester(requester)

    fun request() {
        requests++
    }

    suspend fun reveal() {
        delay(EXPAND_ANIMATION_MILLIS)
        requester.bringIntoView()
    }
}

@Composable
internal fun rememberRevealOnExpand(): RevealOnExpand {
    val reveal = remember { RevealOnExpand(BringIntoViewRequester()) }
    LaunchedEffect(reveal.requests) {
        if (reveal.requests > 0) reveal.reveal()
    }
    return reveal
}

private const val EXPAND_ANIMATION_MILLIS = 350L
