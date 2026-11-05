package io.devdeck.storage

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import io.devdeck.internal.DeckSettings

/**
 * Decides where each SharedPreferences file shows up in the browser. Each entry is a file name without `.xml`, or a
 * prefix ending in `*` (`"analytics_*"`).
 *
 * - [app]: always listed with the app's own files, even if it looks like a library file.
 * - [library]: listed only after *Show library files* is turned on.
 * - [hidden]: never listed, for files nobody should edit from the drawer.
 *
 * Files that match none of these are sorted automatically: WebView, Firebase, Google Play services, AndroidX, DevDeck
 * and package-style names from other organisations count as library files. Testers can also move a file between the
 * two lists from its page; their choice wins over [app] and [library] but not over [hidden].
 */
@Immutable
public data class PreferenceFileRules(
    val app: Set<String> = emptySet(),
    val library: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
)

internal enum class PreferenceFileKind { App, Library, Hidden }

internal fun PreferenceFileRules.kindOf(name: String, packageName: String, overrides: PreferenceOverrides.Snapshot): PreferenceFileKind =
    when {
        hidden.matches(name) -> PreferenceFileKind.Hidden
        name in overrides.app -> PreferenceFileKind.App
        name in overrides.library -> PreferenceFileKind.Library
        app.matches(name) -> PreferenceFileKind.App
        library.matches(name) -> PreferenceFileKind.Library
        LibraryPreferences.isLibrary(name, packageName) -> PreferenceFileKind.Library
        else -> PreferenceFileKind.App
    }

private fun Set<String>.matches(name: String): Boolean =
    any { pattern -> if (pattern.endsWith('*')) name.startsWith(pattern.dropLast(1)) else name == pattern }

/** Files a tester moved between the app and library lists from the drawer, kept in DevDeck's settings. */
internal object PreferenceOverrides {

    data class Snapshot(val app: Set<String> = emptySet(), val library: Set<String> = emptySet())

    var current: Snapshot by mutableStateOf(Snapshot())
        private set

    /** The rules of the browser that was shown last, so a file's page sorts it the same way. */
    var rules: PreferenceFileRules = PreferenceFileRules()

    private var loaded = false

    /** The current overrides, read as Compose state so callers update when a file is moved. */
    fun observe(context: Context): Snapshot {
        load(context)
        return current
    }

    private fun load(context: Context): Snapshot {
        if (!loaded) {
            loaded = true
            val prefs = DeckSettings.of(context)
            current = Snapshot(
                app = prefs.getStringSet(KEY_APP, null).orEmpty().toSet(),
                library = prefs.getStringSet(KEY_LIBRARY, null).orEmpty().toSet(),
            )
        }
        return current
    }

    fun move(context: Context, name: String, to: PreferenceFileKind) {
        val now = load(context)
        current = when (to) {
            PreferenceFileKind.App -> Snapshot(app = now.app + name, library = now.library - name)
            PreferenceFileKind.Library -> Snapshot(app = now.app - name, library = now.library + name)
            PreferenceFileKind.Hidden -> return
        }
        DeckSettings.of(context).edit {
            putStringSet(KEY_APP, current.app)
            putStringSet(KEY_LIBRARY, current.library)
        }
    }

    private const val KEY_APP = "preference_files_app"
    private const val KEY_LIBRARY = "preference_files_library"
}
