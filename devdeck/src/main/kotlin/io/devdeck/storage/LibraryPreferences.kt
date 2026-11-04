package io.devdeck.storage

/**
 * Tells the app's own SharedPreferences files apart from the ones libraries and the system write into the same folder
 * (WebView, Firebase, Google Play services, AndroidX, DevDeck itself).
 */
internal object LibraryPreferences {

    /** True when [name] belongs to a library rather than to the app with package [packageName]. */
    fun isLibrary(name: String, packageName: String): Boolean {
        if (LIBRARY_PREFIXES.any { name.startsWith(it) }) return true
        // Files named like a package (com.google.foo, androidx.work.util) belong to whoever owns that package.
        if (!QUALIFIED_NAME.matches(name)) return false
        return !name.startsWith(organisation(packageName))
    }

    /** The first two segments of the package, so com.example.app and com.example.app.debug both own com.example.*. */
    private fun organisation(packageName: String): String = packageName.split('.').take(2).joinToString(".", postfix = ".")

    private val QUALIFIED_NAME = Regex("^[a-z][a-z0-9_]*(\\.[A-Za-z0-9_]+)+.*")

    private val LIBRARY_PREFIXES = listOf(
        "devdeck_",
        "androidx.",
        "android.",
        "com.android.",
        "com.google.",
        "WebView",
        "Firebase",
        "PersistedInstallation",
        "com.facebook.",
        "com.crashlytics",
        "com.datadog.",
        "io.sentry",
    )
}
