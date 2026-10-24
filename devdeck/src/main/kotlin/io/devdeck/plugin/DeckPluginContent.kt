package io.devdeck.plugin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import io.devdeck.components.CollapsibleSection
import io.devdeck.components.SectionDivider
import io.devdeck.components.SectionTitle

@Composable
public fun DeckPluginContent(plugins: List<DeckPlugin>, modifier: Modifier = Modifier, collapsibleSections: Boolean = true) {
    val groups = plugins.fold(mutableListOf<Pair<String?, MutableList<DeckPlugin>>>()) { acc, plugin ->
        val last = acc.lastOrNull()
        if (last != null && last.first != null && last.first == plugin.section) {
            last.second.add(plugin)
        } else {
            acc.add(plugin.section to mutableListOf(plugin))
        }
        acc
    }
    Column(modifier.fillMaxWidth()) {
        groups.forEachIndexed { index, (section, sectionPlugins) ->
            if (index > 0) SectionDivider()
            val pluginsContent: @Composable () -> Unit = { sectionPlugins.forEach { plugin -> key(plugin.id) { plugin.Content() } } }
            when {
                section == null -> pluginsContent()
                collapsibleSections -> CollapsibleSection(section) { pluginsContent() }
                else -> {
                    SectionTitle(section)
                    pluginsContent()
                }
            }
        }
    }
}
