package io.devdeck.plugin

import androidx.compose.runtime.Composable
import io.devdeck.appearance.DisplaySettingsButton
import io.devdeck.appearance.LanguageSelector
import io.devdeck.appearance.NightModeSelector

public class AppearancePlugin(
    private val nightMode: Boolean = true,
    private val languageTags: List<String> = emptyList(),
    private val displaySettings: Boolean = true,
    override val section: String? = "Appearance 🎨",
) : DeckPlugin {

    override val id: String = "appearance"

    @Composable
    override fun Content() {
        if (nightMode) NightModeSelector()
        if (languageTags.isNotEmpty()) LanguageSelector(languageTags)
        if (displaySettings) DisplaySettingsButton()
    }
}
