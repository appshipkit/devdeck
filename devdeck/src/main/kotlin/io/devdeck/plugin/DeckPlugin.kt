package io.devdeck.plugin

import android.content.Context
import androidx.compose.runtime.Composable

public interface DeckPlugin {
    public val id: String

    public val section: String? get() = null

    public fun onAttach(context: Context) {}

    @Composable
    public fun Content()
}

public fun deckPlugin(id: String, section: String? = null, onAttach: (Context) -> Unit = {}, content: @Composable () -> Unit): DeckPlugin =
    LambdaPlugin(id, section, onAttach, content)

private class LambdaPlugin(
    override val id: String,
    override val section: String?,
    private val attach: (Context) -> Unit,
    private val content: @Composable () -> Unit,
) : DeckPlugin {
    override fun onAttach(context: Context) = attach(context)

    @Composable
    override fun Content() = content()
}
