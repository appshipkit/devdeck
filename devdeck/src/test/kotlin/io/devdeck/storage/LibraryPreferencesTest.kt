package io.devdeck.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryPreferencesTest {

    private fun isLibrary(name: String) = LibraryPreferences.isLibrary(name, packageName = "com.example.shop.debug")

    @Test
    fun `keeps the app's own files`() {
        listOf("user_session", "settings", "onboarding", "com.example.shop.debug_preferences", "com.example.cart").forEach {
            assertFalse(it, isLibrary(it))
        }
    }

    @Test
    fun `hides library and system files`() {
        listOf(
            "WebViewChromiumPrefs",
            "WebViewProfilePrefsDefault",
            "FirebaseHeartBeatW0RFRkFVTFRd",
            "PersistedInstallation.W0RFRkFVTFRd",
            "com.google.android.gms.measurement.prefs",
            "com.google.firebase.crashlytics",
            "androidx.work.util",
            "devdeck_settings",
            "com.facebook.sdk.appEventPreferences",
            "io.sentry.prefs",
            "org.example.analytics",
        ).forEach { assertTrue(it, isLibrary(it)) }
    }
}
