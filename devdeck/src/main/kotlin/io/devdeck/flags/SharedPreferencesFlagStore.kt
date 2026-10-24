package io.devdeck.flags

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit

public class SharedPreferencesFlagStore(context: Context, name: String = "devdeck_flags") : FeatureFlagStore {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    private val overrides = mutableStateMapOf<String, String>().apply {
        prefs.all.forEach { (key, value) -> if (value is String) put(key, value) }
    }

    override fun <T : Any> get(flag: FeatureFlag<T>): T = overrides[flag.key]?.let(flag::decode) ?: flag.default

    override fun <T : Any> set(flag: FeatureFlag<T>, value: T) {
        val raw = flag.encode(value)
        overrides[flag.key] = raw
        prefs.edit { putString(flag.key, raw) }
    }

    override fun isOverridden(flag: FeatureFlag<*>): Boolean = flag.key in overrides

    override fun reset(flag: FeatureFlag<*>) {
        overrides.remove(flag.key)
        prefs.edit { remove(flag.key) }
    }
}
