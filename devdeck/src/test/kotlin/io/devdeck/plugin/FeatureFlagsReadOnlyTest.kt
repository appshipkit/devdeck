package io.devdeck.plugin

import io.devdeck.flags.FeatureFlag
import io.devdeck.flags.FeatureFlagStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsReadOnlyTest {

    private val checkout = FeatureFlag.Toggle("new_checkout", default = false, section = "Ready", category = "checkout")
    private val payments = FeatureFlag.Toggle("saved_cards", default = false, section = "Ready", category = "payment")
    private val growth = FeatureFlag.Toggle("live_shopping", default = false, section = "In development", category = "growth")
    private val locked = FeatureFlag.Toggle("kill_switch", default = false, category = "platform", editable = false)
    private val all = listOf(checkout, payments, growth, locked)

    private class MapStore : FeatureFlagStore {
        val values = mutableMapOf<String, Any>()

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> get(flag: FeatureFlag<T>): T = (values[flag.key] ?: flag.default) as T

        override fun <T : Any> set(flag: FeatureFlag<T>, value: T) {
            values[flag.key] = value
        }

        override fun isOverridden(flag: FeatureFlag<*>): Boolean = flag.key in values

        override fun reset(flag: FeatureFlag<*>) {
            values.remove(flag.key)
        }
    }

    @Test
    fun `everything is editable by default except flags defined as read-only`() {
        val plugin = FeatureFlagsPlugin(all, MapStore())
        assertEquals(listOf(true, true, true, false), all.map(plugin::canEdit))
    }

    @Test
    fun `editing can be turned off for all flags, categories or sections, or single flags`() {
        assertTrue(all.none(FeatureFlagsPlugin(all, MapStore(), editable = false)::canEdit))

        val byCategory = FeatureFlagsPlugin(all, MapStore(), readOnlyCategories = setOf("Checkout", "in development"))
        assertEquals(listOf(false, true, false, false), all.map(byCategory::canEdit))

        val byKey = FeatureFlagsPlugin(all, MapStore(), readOnlyFlags = setOf("saved_cards"))
        assertEquals(listOf(true, false, true, false), all.map(byKey::canEdit))
    }

    @Test
    fun `import skips read-only flags`() {
        val store = MapStore()
        val plugin = FeatureFlagsPlugin(all, store, readOnlyFlags = setOf("saved_cards"))

        val imported = plugin.importJson("""{"new_checkout": true, "saved_cards": true}""")

        assertEquals(1, imported)
        assertTrue(store[checkout])
        assertFalse(store[payments])
    }
}
