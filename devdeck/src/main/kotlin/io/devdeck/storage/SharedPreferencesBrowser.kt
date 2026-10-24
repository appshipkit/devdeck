package io.devdeck.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import io.devdeck.LocalDeckState
import io.devdeck.components.BulletItem
import io.devdeck.components.ConfirmButtonItem
import io.devdeck.components.ConfirmDeleteButton
import io.devdeck.components.InfoGroup
import io.devdeck.components.JsonInputItem
import io.devdeck.components.NumberInputItem
import io.devdeck.components.PageLinkItem
import io.devdeck.components.SectionDivider
import io.devdeck.components.SwitchItem
import io.devdeck.components.TextInputItem
import io.devdeck.components.TextItem
import io.devdeck.internal.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
public fun SharedPreferencesBrowser(modifier: Modifier = Modifier, include: (String) -> Boolean = { true }) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    val loaded by produceState<List<String>?>(null, context, version) {
        value = withContext(Dispatchers.IO) { StorageFiles.sharedPreferencesNames(context) }
    }
    val names = remember(loaded, include) { loaded.orEmpty().filter(include) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (names.size == 1) "1 file" else "${names.size} files",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ConfirmDeleteButton(
                contentDescription = "Clear all files",
                onConfirm = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            names.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit(commit = true) { clear() } }
                        }
                        version++
                    }
                },
                enabled = names.isNotEmpty(),
            )
        }
        if (loaded != null && names.isEmpty()) TextItem("No SharedPreferences files")
        InfoGroup(dividerThreshold = 2) { names.forEach { name -> key(name, version) { PreferenceFileRow(name) } } }
    }
}

@Composable
private fun PreferenceFileRow(name: String) {
    val prefs = rememberObservedPreferences(name)
    val count = prefs?.values?.all?.size
    PageLinkItem(
        title = name,
        subtitle = when (count) {
            null -> "…"
            1 -> "1 key"
            else -> "$count keys"
        },
    ) { SharedPreferencesFile(name) }
}

@Composable
public fun SharedPreferencesFile(name: String, modifier: Modifier = Modifier) {
    val prefs = rememberObservedPreferences(name) ?: return
    val entries = prefs.values.all.toSortedMap()
    Column(modifier.fillMaxWidth()) {
        if (entries.isEmpty()) TextItem("This file is empty")
        InfoGroup(dividerThreshold = 2) {
            entries.forEach { (key, value) ->
                PageLinkItem(title = key, subtitle = "${value.typeName()} · ${value.preview()}") { PreferenceEditor(name, key) }
            }
        }
        SectionDivider()
        ConfirmButtonItem(title = "Clear all keys", onConfirm = {
            prefs.values.edit { clear() }
            prefs.refresh()
        })
    }
}

@Composable
private fun PreferenceEditor(name: String, key: String) {
    val prefs = rememberObservedPreferences(name) ?: return
    val drawerState = LocalDeckState.current
    val editor = prefs.values
    when (val value = editor.all[key]) {
        null -> TextItem("This key no longer exists")

        is Boolean -> SwitchItem(key, checked = value, subtitle = "Boolean", onCheckedChange = {
            editor.edit { putBoolean(key, it) }
        })

        is Int -> NumberInputItem(key, value = value.toLong(), onValueCommit = { editor.edit { putInt(key, it.toInt()) } })

        is Long -> NumberInputItem(key, value = value, onValueCommit = { editor.edit { putLong(key, it) } })

        is Float -> NumberInputItem(key, value = value.toDouble(), onValueCommit = { editor.edit { putFloat(key, it.toFloat()) } })

        is String -> if (Json.isJson(value)) {
            JsonInputItem(key, value = value, onValueCommit = { editor.edit { putString(key, it) } })
        } else {
            TextInputItem(key, value = value, singleLine = false, onValueCommit = { editor.edit { putString(key, it) } })
        }

        is Set<*> -> value.forEach { BulletItem(it.toString()) }

        else -> BulletItem(key, value.toString())
    }
    SectionDivider()
    ConfirmButtonItem(title = "Delete key", onConfirm = {
        editor.edit { remove(key) }
        drawerState?.navigateBack()
    })
}

private class ObservedPreferences(val values: SharedPreferences, val version: Int, val refresh: () -> Unit)

/** Opens and loads the file off the main thread; null until it is ready. */
@Composable
private fun rememberObservedPreferences(name: String): ObservedPreferences? {
    val context = LocalContext.current
    val prefs by produceState<SharedPreferences?>(null, name) {
        value = withContext(Dispatchers.IO) { context.getSharedPreferences(name, Context.MODE_PRIVATE).also { it.all } }
    }
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val current = prefs
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        current?.registerOnSharedPreferenceChangeListener(listener)
        onDispose { current?.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return prefs?.let { ObservedPreferences(it, version) { version++ } }
}

private fun Any?.typeName(): String = when (this) {
    is Boolean -> "Boolean"
    is Int -> "Int"
    is Long -> "Long"
    is Float -> "Float"
    is String -> if (Json.isJson(this)) "JSON" else "String"
    is Set<*> -> "String set"
    else -> "Unknown"
}

// Only bounds the text handed to layout; the subtitle's maxLines does the visible truncation.
private fun Any?.preview(): String = toString().take(MAX_PREVIEW_CHARS).replace('\n', ' ')

private const val MAX_PREVIEW_CHARS = 300
