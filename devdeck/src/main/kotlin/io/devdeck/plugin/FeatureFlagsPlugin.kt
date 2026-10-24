package io.devdeck.plugin

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.devdeck.actions.AppActions
import io.devdeck.components.ButtonItem
import io.devdeck.components.ConfirmButtonItem
import io.devdeck.components.JsonInputItem
import io.devdeck.components.NumberInputItem
import io.devdeck.components.PageLinkItem
import io.devdeck.components.Section
import io.devdeck.components.SectionDivider
import io.devdeck.components.SwitchItem
import io.devdeck.components.TextInputItem
import io.devdeck.flags.FeatureFlag
import io.devdeck.flags.FeatureFlagStore
import io.devdeck.internal.copyToClipboard
import io.devdeck.internal.findActivity
import io.devdeck.internal.readClipboardText
import io.devdeck.logs.DeckLogger
import org.json.JSONObject

public class FeatureFlagsPlugin(
    private val flags: List<FeatureFlag<*>>,
    private val store: FeatureFlagStore,
    private val title: String = "Feature flags",
    override val section: String? = "Configuration 🔧",
    private val bulkEdit: Boolean = true,
) : DeckPlugin {

    override val id: String = "feature_flags"

    @Composable
    override fun Content() {
        PageLinkItem(title = title) { FlagSections() }
    }

    @Composable
    private fun FlagSections() {
        val context = LocalContext.current
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
                subtitle = "Relaunches the app in a new process",
                onClick = { AppActions.restart(context) },
            )
        }
        SectionDivider()
        if (bulkEdit) {
            Section("Bulk edit ✏️") {
                ConfirmButtonItem(
                    title = "Reset all flags",
                    subtitle = "Remove every override",
                    confirmTitle = "Tap again to reset all flags",
                    onConfirm = {
                        flags.forEach(store::reset)
                        DeckLogger.log("Feature flags reset")
                    },
                )
                ButtonItem(title = "Import from clipboard", onClick = { importFromClipboard(context) })
                ButtonItem(title = "Export to clipboard", onClick = { exportToClipboard(context) })
            }
        }
    }

    @Composable
    private fun FlagList(categoryFlags: List<FeatureFlag<*>>) {
        categoryFlags.forEach { flag ->
            when (flag) {
                is FeatureFlag.Toggle -> SwitchItem(
                    title = flag.title,
                    checked = store[flag],
                    enabled = flag.editable,
                    onCheckedChange = { store[flag] = it },
                )

                is FeatureFlag.Text -> TextInputItem(
                    title = flag.title,
                    value = store[flag],
                    enabled = flag.editable,
                    onValueCommit = { store[flag] = it },
                )

                is FeatureFlag.Number -> NumberInputItem(
                    title = flag.title,
                    value = store[flag],
                    enabled = flag.editable,
                    allowDecimals = !flag.wholeNumber,
                    range = flag.range,
                    onValueCommit = { store[flag] = it },
                )

                is FeatureFlag.Json -> JsonInputItem(
                    title = flag.title,
                    value = store[flag],
                    enabled = flag.editable,
                    onValueCommit = { store[flag] = it },
                )
            }
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
        flags.forEach { flag ->
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

private fun <T : Any> FeatureFlag<T>.exportValue(store: FeatureFlagStore): Any = toJson(store[this])

private fun <T : Any> FeatureFlag<T>.importValue(store: FeatureFlagStore, value: Any): Boolean {
    val decoded = decode(value.toString()) ?: return false
    store[this] = decoded
    return true
}
