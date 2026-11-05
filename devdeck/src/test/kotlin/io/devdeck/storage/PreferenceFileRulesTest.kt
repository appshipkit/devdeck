package io.devdeck.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceFileRulesTest {

    private val packageName = "com.example.shop"
    private val none = PreferenceOverrides.Snapshot()

    @Test
    fun `sorts files automatically without rules`() {
        val rules = PreferenceFileRules()
        assertEquals(PreferenceFileKind.App, rules.kindOf("user_session", packageName, none))
        assertEquals(PreferenceFileKind.Library, rules.kindOf("WebViewChromiumPrefs", packageName, none))
    }

    @Test
    fun `rules move files between lists and hide them`() {
        val rules = PreferenceFileRules(
            app = setOf("com.google.maps.cache"),
            library = setOf("analytics_*"),
            hidden = setOf("secrets"),
        )
        assertEquals(PreferenceFileKind.App, rules.kindOf("com.google.maps.cache", packageName, none))
        assertEquals(PreferenceFileKind.Library, rules.kindOf("analytics_events", packageName, none))
        assertEquals(PreferenceFileKind.App, rules.kindOf("analytics", packageName, none))
        assertEquals(PreferenceFileKind.Hidden, rules.kindOf("secrets", packageName, none))
    }

    @Test
    fun `a tester's choice wins over rules but not over hidden`() {
        val rules = PreferenceFileRules(library = setOf("analytics_*"), hidden = setOf("secrets"))
        val overrides = PreferenceOverrides.Snapshot(app = setOf("analytics_events", "secrets"), library = setOf("user_session"))
        assertEquals(PreferenceFileKind.App, rules.kindOf("analytics_events", packageName, overrides))
        assertEquals(PreferenceFileKind.Library, rules.kindOf("user_session", packageName, overrides))
        assertEquals(PreferenceFileKind.Hidden, rules.kindOf("secrets", packageName, overrides))
    }
}
