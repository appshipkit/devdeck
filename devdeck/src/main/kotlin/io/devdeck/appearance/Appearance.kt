package io.devdeck.appearance

import android.app.LocaleManager
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import io.devdeck.components.ButtonItem
import io.devdeck.components.DropdownItem
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckSettings
import java.util.Locale

public enum class NightModeOption(public val label: String, internal val mode: Int) {
    System("Follow system", UiModeManager.MODE_NIGHT_AUTO),
    Light("Light", UiModeManager.MODE_NIGHT_NO),
    Dark("Dark", UiModeManager.MODE_NIGHT_YES),
}

@Composable
public fun NightModeSelector(modifier: Modifier = Modifier, title: String = "Dark mode") {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        TextItem("$title override needs Android 12 or newer", modifier)
        return
    }
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager }
    val prefs = remember(context) { DeckSettings.of(context) }
    var selected by remember {
        mutableStateOf(NightModeOption.entries.firstOrNull { it.name == prefs.getString(KEY_NIGHT_MODE, null) } ?: NightModeOption.System)
    }
    DropdownItem(
        title = title,
        options = NightModeOption.entries,
        selected = selected,
        onSelected = { option ->
            selected = option
            prefs.edit { putString(KEY_NIGHT_MODE, option.name) }
            manager.setApplicationNightMode(option.mode)
        },
        optionLabel = { it.label },
        modifier = modifier,
    )
}

@Composable
public fun LanguageSelector(languageTags: List<String>, modifier: Modifier = Modifier, title: String = "App language") {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        TextItem("$title override needs Android 13 or newer", modifier)
        return
    }
    val context = LocalContext.current
    val options = remember(languageTags) { listOf(SYSTEM_LANGUAGE) + languageTags }
    var selected by remember { mutableStateOf(currentLanguage(context)) }
    DropdownItem(
        title = title,
        options = options,
        selected = selected,
        onSelected = { tag ->
            selected = tag
            setLanguage(context, tag)
        },
        optionLabel = { tag ->
            if (tag ==
                SYSTEM_LANGUAGE
            ) {
                "Follow system"
            } else {
                Locale.forLanguageTag(tag).let { "${it.displayName} ($tag)" }
            }
        },
        modifier = modifier,
    )
}

@Composable
public fun DisplaySettingsButton(modifier: Modifier = Modifier, title: String = "Display & font size settings") {
    val context = LocalContext.current
    ButtonItem(
        title = title,
        modifier = modifier,
        onClick = { openDisplaySettings(context) },
    )
}

private fun openDisplaySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun currentLanguage(context: Context): String =
    context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags().ifEmpty { SYSTEM_LANGUAGE }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setLanguage(context: Context, tag: String) {
    context.getSystemService(LocaleManager::class.java).applicationLocales =
        if (tag == SYSTEM_LANGUAGE) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
}

private const val SYSTEM_LANGUAGE = "system"
private const val KEY_NIGHT_MODE = "appearance.night_mode"
