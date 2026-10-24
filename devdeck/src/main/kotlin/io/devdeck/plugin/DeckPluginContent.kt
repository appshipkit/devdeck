package io.devdeck.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.devdeck.components.DeckCard
import io.devdeck.components.SectionCard

@Composable
public fun DeckPluginContent(
    plugins: List<DeckPlugin>,
    modifier: Modifier = Modifier,
    collapsibleSections: Boolean = true,
    oneSectionAtATime: Boolean = false,
) {
    val groups = plugins.fold(mutableListOf<Pair<String?, MutableList<DeckPlugin>>>()) { acc, plugin ->
        val last = acc.lastOrNull()
        if (last != null && last.first != null && last.first == plugin.section) {
            last.second.add(plugin)
        } else {
            acc.add(plugin.section to mutableListOf(plugin))
        }
        acc
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        groups.forEach { (section, sectionPlugins) ->
            val pluginsContent: @Composable () -> Unit = { sectionPlugins.forEach { plugin -> key(plugin.id) { plugin.Content() } } }
            if (section == null) {
                DeckCard { pluginsContent() }
            } else {
                key(section) {
                    SectionCard(section, collapsible = collapsibleSections, closeOthersOnOpen = oneSectionAtATime) { pluginsContent() }
                }
            }
        }
    }
}
