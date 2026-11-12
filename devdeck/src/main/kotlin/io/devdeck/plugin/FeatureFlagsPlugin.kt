package io.devdeck.plugin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.devdeck.actions.AppActions
import io.devdeck.components.ButtonItem
import io.devdeck.components.ConfirmButtonItem
import io.devdeck.components.JsonInputItem
import io.devdeck.components.NumberInputItem
import io.devdeck.components.PageLinkItem
import io.devdeck.components.Section
import io.devdeck.components.SectionDivider
import io.devdeck.components.SectionTitle
import io.devdeck.components.SwitchItem
import io.devdeck.components.TextInputItem
import io.devdeck.components.TextItem
import io.devdeck.flags.FeatureFlag
import io.devdeck.flags.FeatureFlagStore
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.copyToClipboard
import io.devdeck.internal.findActivity
import io.devdeck.internal.readClipboardText
import io.devdeck.logs.DeckLogger
import org.json.JSONObject

/**
 * Lists [flags] by section and category, with search, restart buttons and bulk edit.
 *
 * Editing can be turned off for every flag ([editable] = false), for whole categories or sections
 * ([readOnlyCategories], matched by name, ignoring case) or for single flags ([readOnlyFlags], by key). Read-only
 * flags still show their current value, are skipped by *Reset all flags* and *Import*, and are still exported.
 */
public class FeatureFlagsPlugin(
    private val flags: List<FeatureFlag<*>>,
    private val store: FeatureFlagStore,
    private val title: String = "Feature flags",
    override val section: String? = "Configuration 🔧",
    private val bulkEdit: Boolean = true,
    private val editable: Boolean = true,
    private val readOnlyCategories: Set<String> = emptySet(),
    private val readOnlyFlags: Set<String> = emptySet(),
) : DeckPlugin {

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        flags: List<FeatureFlag<*>>,
        store: FeatureFlagStore,
        title: String = "Feature flags",
        section: String? = "Configuration 🔧",
        bulkEdit: Boolean = true,
    ) : this(flags, store, title, section, bulkEdit, editable = true)

    /** False when [flag] is read-only, by its own definition or by this plugin's settings. */
    internal fun canEdit(flag: FeatureFlag<*>): Boolean = flag.editable &&
        editable &&
        flag.key !in readOnlyFlags &&
        readOnlyCategories.none { it.equals(flag.category, ignoreCase = true) || it.equals(flag.section, ignoreCase = true) }

    override val id: String = "feature_flags"

    @Composable
    override fun Content() {
        PageLinkItem(title = title) { FlagSections() }
    }

    @Composable
    private fun FlagSections() {
        val context = LocalContext.current
        var query by rememberSaveable { mutableStateOf("") }
        FlagSearchField(query = query, onQueryChange = { query = it })
        if (query.isNotBlank()) {
            SearchResults(query.trim())
            return
        }
        flags.groupBy { it.section }.forEach { (section, sectionFlags) ->
            Section(section) {
                sectionFlags.groupBy { it.category }.forEach { (category, categoryFlags) ->
                    PageLinkItem(title = category.replaceFirstChar { it.titlecase() }) { FlagList(categoryFlags) }
                }
            }
            SectionDivider()
        }
        // Most apps read flags once per screen or process, so changes show up after a restart.
        Section("Apply changes 🔄") {
            ButtonItem(
                title = "Restart screen",
                subtitle = "Recreates the current screen",
                onClick = { context.findActivity()?.recreate() },
            )
            ButtonItem(
                title = "Restart app",
                subtitle = "Starts the app again",
                onClick = { AppActions.restart(context) },
            )
        }
        SectionDivider()
        if (bulkEdit) {
            val anyEditable = flags.any(::canEdit)
            Section("Bulk edit ✏️") {
                if (anyEditable) {
                    ConfirmButtonItem(
                        title = "Reset all flags",
                        subtitle = "Remove every override",
                        confirmTitle = "Tap again to reset all flags",
                        onConfirm = {
                            flags.filter(::canEdit).forEach(store::reset)
                            DeckLogger.log("Feature flags reset")
                        },
                    )
                }
                if (anyEditable) ButtonItem(title = "Import from clipboard", onClick = { importFromClipboard(context) })
                ButtonItem(title = "Export to clipboard", onClick = { exportToClipboard(context) })
            }
        }
    }

    // Same outlined look as the deep link field, so the drawer's input fields match.
    @Composable
    private fun FlagSearchField(query: String, onQueryChange: (String) -> Unit) {
        val focus = LocalFocusManager.current
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search feature flags", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            trailingIcon = {
                if (query.isEmpty()) {
                    Icon(DeckIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    IconButton(onClick = { onQueryChange("") }) { Icon(DeckIcons.Close, contentDescription = "Clear search") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }

    @Composable
    private fun SearchResults(query: String) {
        val matches = remember(query) { flags.filter { it.matches(query) } }
        if (matches.isEmpty()) {
            TextItem("No flags match \"$query\"")
            return
        }
        // Grouped like the regular pages, with the key under each title so a match by key is easy to spot.
        matches.groupBy { "${it.section} › ${it.category.replaceFirstChar { c -> c.titlecase() }}" }.forEach { (group, groupFlags) ->
            SectionTitle(group)
            groupFlags.forEach { flag -> key(flag.key) { FlagRow(flag, flag.key.takeIf { it != flag.title }) } }
        }
    }

    @Composable
    private fun FlagList(categoryFlags: List<FeatureFlag<*>>) {
        categoryFlags.forEach { flag -> FlagRow(flag, detail = null) }
    }

    @Composable
    private fun FlagRow(flag: FeatureFlag<*>, detail: String?) {
        val enabled = canEdit(flag)
        val subtitle = listOfNotNull(detail, if (enabled) null else "Read-only").joinToString(" · ").ifEmpty { null }
        when (flag) {
            is FeatureFlag.Toggle -> SwitchItem(
                title = flag.title,
                checked = store[flag],
                enabled = enabled,
                subtitle = subtitle,
                onCheckedChange = { store[flag] = it },
            )

            is FeatureFlag.Text -> TextInputItem(
                title = flag.title,
                value = store[flag],
                enabled = enabled,
                subtitle = subtitle,
                onValueCommit = { store[flag] = it },
            )

            is FeatureFlag.Number -> NumberInputItem(
                title = flag.title,
                value = store[flag],
                enabled = enabled,
                allowDecimals = !flag.wholeNumber,
                range = flag.range,
                subtitle = subtitle,
                onValueCommit = { store[flag] = it },
            )

            is FeatureFlag.Json -> JsonInputItem(
                title = flag.title,
                value = store[flag],
                enabled = enabled,
                subtitle = subtitle,
                onValueCommit = { store[flag] = it },
            )
        }
    }

    public fun exportJson(): String {
        val json = JSONObject()
        flags.forEach { flag -> json.put(flag.key, flag.exportValue(store)) }
        return json.toString(2)
    }

    public fun importJson(raw: String): Int {
        val json = JSONObject(raw)
        var count = 0
        flags.filter(::canEdit).forEach { flag ->
            if (json.has(flag.key) && flag.importValue(store, json.get(flag.key))) count++
        }
        return count
    }

    private fun exportToClipboard(context: Context) {
        val json = exportJson()
        context.copyToClipboard("Feature flags", json)
        DeckLogger.log("Feature flags exported", payload = json)
    }

    private fun importFromClipboard(context: Context) {
        val text = context.readClipboardText()
        val message = runCatching { "Imported ${importJson(text.orEmpty())} flags" }.getOrElse { "Clipboard does not contain flag JSON" }
        DeckLogger.log(message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

/** Case-insensitive match on the title or key; `checkout_v2`, `checkout v2` and `CheckoutV2` all find the same flag. */
internal fun FeatureFlag<*>.matches(query: String): Boolean {
    val needle = query.flatten()
    return needle.isNotEmpty() && (title.flatten().contains(needle) || key.flatten().contains(needle))
}

private fun String.flatten(): String = filter { it.isLetterOrDigit() }.lowercase()

private fun <T : Any> FeatureFlag<T>.exportValue(store: FeatureFlagStore): Any = toJson(store[this])

private fun <T : Any> FeatureFlag<T>.importValue(store: FeatureFlagStore, value: Any): Boolean {
    val decoded = decode(value.toString()) ?: return false
    store[this] = decoded
    return true
}
