# DevDeck

[![CI](https://github.com/appshipkit/devdeck/actions/workflows/ci.yml/badge.svg?branch=main&event=push)](https://github.com/appshipkit/devdeck/actions/workflows/ci.yml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.appshipkit/devdeck)](https://central.sonatype.com/artifact/io.github.appshipkit/devdeck)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A debug drawer for Android apps, built with Jetpack Compose. Open it with a two-finger swipe, a floating button, a keyboard shortcut or code, and get your team's debug tools in one place:

- **Configure:** switch endpoints and override feature flags.
- **Inspect:** network calls (REST and GraphQL), your logs, logcat, Timber and crashes.
- **Simulate:** a slow, flaky or offline network, and low memory.
- **Capture:** screenshots, long screenshots, screen recordings and bug reports.
- **Ship:** tell testers when a newer test build is available.

Everything is a plugin: pick the ones you need or write your own in a few lines. DevDeck works on Compose and View screens, and stays out of release builds when added with `debugImplementation`.

## Contents

- [Quick start](#quick-start)
- [Opening the drawer](#opening-the-drawer)
- [Plugins](#plugins)
- [Features](#features)
- [Extending DevDeck](#extending-devdeck)
- [Reference](#reference)
- [Security](#security)
- [Sample app, contributing and license](#sample-app)

---

## Quick start

### 1. Add the dependencies

DevDeck is on Maven Central, so `mavenCentral()` (in every new Android project) is all you need.

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation("io.github.appshipkit:devdeck:LATEST_VERSION")
    debugImplementation("io.github.appshipkit:devdeck-okhttp:LATEST_VERSION")           // optional: network inspector (OkHttp, Apollo)
    debugImplementation("io.github.appshipkit:devdeck-timber:LATEST_VERSION")           // optional: Timber logs
    debugImplementation("io.github.appshipkit:devdeck-appdistribution:LATEST_VERSION")  // optional: Firebase test build updates
}
```

Replace `LATEST_VERSION` with the version on the Maven Central badge above, for example `1.1.0`.

<details>
<summary>With a version catalog</summary>

```toml
# gradle/libs.versions.toml
[versions]
devdeck = "LATEST_VERSION"

[libraries]
devdeck = { module = "io.github.appshipkit:devdeck", version.ref = "devdeck" }
devdeck-okhttp = { module = "io.github.appshipkit:devdeck-okhttp", version.ref = "devdeck" }
```
</details>

### 2. Install it

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DevDeck.install(this)
    }
}
```

Every activity now has a drawer with the default plugins.

### 3. Capture network calls (optional)

```kotlin
OkHttpClient.Builder()
    .addInterceptor(DevDeckOkHttpInterceptor())   // addInterceptor, not addNetworkInterceptor
    .build()
```

For Timber, add `TimberLogsPlugin()` to your plugins; it plants its own tree.

### 4. Keep it out of release builds

> [!WARNING]
> DevDeck doesn't check the build type at runtime. Added with `implementation`, it ships in your release build with its network inspector, logs, file browser and app controls. Use `debugImplementation` (or `qaImplementation` for internal build types), never the configuration of the build you publish.

With `debugImplementation`, release code can't reference DevDeck. Keep DevDeck code in the **debug** source set and an empty version in **release**:

```kotlin
// src/debug/kotlin/com/myapp/DebugTools.kt
object DebugTools {
    fun install(app: Application) = DevDeck.install(app, plugins = myPlugins(app))
    fun interceptors(): List<Interceptor> = listOf(DevDeckOkHttpInterceptor())
}

// src/release/kotlin/com/myapp/DebugTools.kt
object DebugTools {
    fun install(app: Application) = Unit
    fun interceptors(): List<Interceptor> = emptyList()
}

// src/main: only ever call DebugTools
DebugTools.install(this)
OkHttpClient.Builder().apply { DebugTools.interceptors().forEach(::addInterceptor) }
```

---

## Opening the drawer

| How | Default | Change it |
|---|---|---|
| Two-finger swipe toward the drawer, anywhere on screen | On | `gestures = DeckGestures(twoFingerSwipe = false)` |
| **Ctrl + Shift + D** (emulator or hardware keyboard) | On | `DeckGestures(keyboardShortcut = DeckShortcut(...))`, or `null` to turn off |
| Floating bug button (drag it anywhere; it stays there) | On | `trigger = DeckTrigger.None` |
| Code | — | `DevDeck.open()`, `DevDeck.close()`, `DevDeck.toggle()` |

- The swipe is detected without taking touches, so taps, scrolls and system back keep working.
- On an emulator, a mouse can't do a two-finger swipe: use Ctrl + Shift + D or the floating button.
- If your app uses two-finger horizontal pans (maps, canvases), turn the swipe off.

The drawer header shows the app version, plus chips for a new test build, an active recording (with a *Stop* button) and any network or memory simulation.

---

## Plugins

`DevDeck.install(this)` uses `DeckPlugins.defaults()`. To choose your own, pass a list. Plugins render top to bottom, grouped into collapsible section cards.

```kotlin
val flags = SharedPreferencesFlagStore(this)

DevDeck.install(
    application = this,
    plugins = listOf(
        EndpointPlugin(listOf(Endpoint("Production", PROD_URL), Endpoint("Staging", STAGING_URL))),
        FeatureFlagsPlugin(Flags.all, flags),
        NetworkSimulatorPlugin(),
        NetworkLogsPlugin(),
        LogcatPlugin(),
        TimberLogsPlugin(),
        LogsPlugin(tags = listOf("General", "Analytics")),
        CrashPlugin(),
        UiToolsPlugin(),
        DevicePlugin(),
        InfoPlugin(buildInfo = { mapOf("Git SHA" to BuildConfig.GIT_SHA) }),
    ),
)
```

| Plugin | Section | What you get | Default |
|---|---|---|---|
| `EndpointPlugin` | Configuration | Endpoint picker, with *Restart app* after a switch | needs endpoints |
| `FeatureFlagsPlugin` | Configuration | Toggle, text, number and JSON flags; restart buttons; reset all, import and export | needs flags |
| `NetworkSimulatorPlugin` | Simulate | Slow or failing network for OkHttp calls | ✓ |
| `MemorySimulatorPlugin` | Simulate | Memory warnings, heap pressure, kill in background | ✓ |
| `NetworkLogsPlugin` | Logs | Network inspector | ✓ |
| `LogcatPlugin` | Logs | Live logcat for your app's process | ✓ |
| `TimberLogsPlugin` | Logs | Every Timber call (`devdeck-timber`) | — |
| `LogsPlugin` | Logs | Your `DeckLogger.log(...)` entries, one row per tag | ✓ |
| `BreadcrumbsPlugin` | Logs | Screens, taps, requests and app events | ✓ |
| `CrashPlugin` | Logs | Saved crashes with stack traces | ✓ |
| `AppActionsPlugin` | App | Deep links, WebView debugging, restart, clear cache or data, kill | ✓ |
| `UpdatesPlugin` | App | New test builds, release notes and install (`devdeck-appdistribution` for Firebase) | needs a source |
| `UiToolsPlugin` | User Interface | Slow animations, keylines, screenshots, long screenshots, recording, gallery | ✓ |
| `AppearancePlugin` | User Interface | Dark mode and app language overrides, system language and display settings | ✓ |
| `PerformancePlugin` | System | Frame rate, jank and memory overlay, memory details | ✓ |
| `StoragePlugin` | System | SharedPreferences browser and editor, storage usage | ✓ |
| `DevicePlugin` | System | Permissions, developer options | ✓ |
| `InfoPlugin` | Info | App info, device info, shareable snapshot, bug report, version | ✓ |

Every plugin takes a `section` (or `null` for no header) and switches to hide rows, e.g. `UiToolsPlugin(screenCapture = false)`.

**Only one Compose screen?** Wrap it instead of installing app-wide: `DevDeckHost(plugins = myPlugins) { AppContent() }`. You can use both; the app-wide drawer steps aside on screens with `DevDeckHost`.

---

## Features

### Endpoints and feature flags

```kotlin
val api = EndpointPlugin(endpoints = listOf(prod, staging))
val baseUrl = api.current(context).url                // read the selected endpoint at startup

object Flags {
    val newCheckout = FeatureFlag.Toggle("new_checkout", default = false, section = "Ready for Testing ✅", category = "checkout")
    val banner = FeatureFlag.Text("banner_text", default = "Free shipping")
    val pageSize = FeatureFlag.Number("page_size", default = 20.0, wholeNumber = true, range = 1.0..100.0)
    val config = FeatureFlag.Json("checkout_config", default = """{"expressPay":true}""")
    val all = listOf(newCheckout, banner, pageSize, config)
}
val enabled: Boolean = flags[Flags.newCheckout]
```

- **Endpoints:** after a switch, *Restart app* applies it (or pass `restartOnChange = true`). For two pickers, give each a `key`: `EndpointPlugin(cdnEndpoints, title = "CDN", key = "cdn")`.
- **Flags:** `section` becomes a header and `category` a sub-page. JSON flags get a preview and a full-page editor. *Restart screen* and *Restart app* apply flags your app reads once.
- **Your own flag system** (LaunchDarkly, Firebase, in-house): implement `FeatureFlagStore` (`get`, `set`, `isOverridden`, `reset`).

### Network inspector

- **Views:** *Timeline*, or *By endpoint* grouped by GraphQL operation or method and path, with counts, failures and timings.
- **Details:** overview, query params, headers and pretty-printed bodies, with Copy URL, Copy cURL and Share.
- **Redaction:** auth, cookie, API-key and CSRF headers, and passwords, tokens, secrets and API keys in URLs and bodies (`NetworkLogger.redactedHeaders` / `redactedFields`). Field names match regardless of case, `_` or `-`.
- **Limits:** the last 300 calls (`maxCalls`), with per-body and total body caps (`maxBodyChars`, `maxTotalBodyChars`). Streaming and untyped bodies pass through uncaptured.

```kotlin
NetworkLogsPlugin(highlights = listOf(
    NetworkHighlight.graphQl(),
    NetworkHighlight.operation("Checkout", Color.Red, "PlaceOrder"),
    NetworkHighlight.urlContains("Payments", Color.Blue, "/payments/"),
))
```

Not using OkHttp? Report calls with `NetworkLogger.start(...)` and `complete(...)` / `fail(...)`.

### Bad network and low memory

| Network condition | What happens |
|---|---|
| Normal | Requests go through untouched |
| Slow | An extra delay (default 3 s) and limited download speed (default 50 KB/s) |
| Failing | *No connection*, *Timeout* or *Server error 503*, without reaching the server |

- Set a *Failure rate* below 100% for a flaky network, and *Only URLs containing* to target calls like `/checkout`.
- The setting survives restarts, and a badge stays on screen while it's on. It works through `DevDeckOkHttpInterceptor` (Apollo included), or from code with `NetworkSimulator.simulation = NetworkSimulation(...)`.
- **Memory:** *Send memory warning* triggers `onTrimMemory` / `onLowMemory`, *Fill memory* holds extra heap, and *Kill app when backgrounded* lets you test restoring state after process death.

### Test build updates

`UpdatesPlugin` shows a *New version* chip in the header (tap it to jump to the details), a dot on the floating button, release notes and an *Install latest* button.

```kotlin
// app/build.gradle.kts, only in the builds testers install
debugImplementation("io.github.appshipkit:devdeck-appdistribution:LATEST_VERSION")
debugImplementation("com.google.firebase:firebase-appdistribution:16.0.0-beta21")

// Application.onCreate
DevDeck.install(this, plugins = DeckPlugins.defaults() + UpdatesPlugin(FirebaseAppDistributionSource(), notify = true))
```

- Needs Firebase set up and testers invited in App Distribution; the first check asks the tester to sign in.
- Checks run on app start and when the drawer opens, at most every 4 hours (`checkIntervalMillis`).
- Not using Firebase? Implement `UpdateSource` (`checkForUpdate()`, `install()`), and `availableBuilds()` for a *Builds* picker.

### Logs and breadcrumbs

| Source | How logs get in |
|---|---|
| `LogsPlugin` | You call `DeckLogger.log("Order placed", tag = "Analytics", payload = json)` |
| `LogcatPlugin` | Everything your app's process writes to logcat; no permission needed |
| `TimberLogsPlugin` | Every `Timber` call, with its tag and level |

Log pages have level filters, search, an *All / By tag* view, pause, share and clear. Common platform noise is hidden; adjust it with `LogcatPlugin(ignoredTags = …, ignoredTagPrefixes = …, ignoredMessagePrefixes = …)`.

**Breadcrumbs** record what led up to a problem: screens, app events, taps (`Modifier.breadcrumb("Place order")` labels Compose controls), requests, and your own steps (`Breadcrumbs.add(...)`). The last 50 are saved with every crash and the last 100 go into bug reports.

### Screen capture, bug reports and info

- **Screenshots and recordings:** under User Interface. While recording, a small floating bar shows the time with pause and stop, so the drawer can stay closed. DevDeck's own floating button and badges are left out of screenshots.
- **Long screenshot (Android 12+):** captures the main scrolling list or page in one image, from what's on screen downwards. It stops at the end, after 5 screens or 8 seconds, or when nothing new appears, so endless feeds stay bounded.
- **Bug report:** a description, recent steps, logs, logcat, device info and chosen captures, sent through the share sheet.
- **App info and device info:** version, package, build type and flavor (from your `BuildConfig`), debuggable, R8 state, SDK levels and device details. The *Device & app snapshot* shares them as one image.
- **Permissions:** each permission's state, with *Request*, *Revoke* (Android 13+) or *Open settings*.

---

## Extending DevDeck

### Your own plugins

```kotlin
val cachePlugin = deckPlugin(id = "cache", section = "System 📱") {
    ButtonItem("Clear image cache", onClick = { imageLoader.memoryCache?.clear() })
    PageLinkItem("Database tables") { TableList() }
}
```

| Building blocks | Use for |
|---|---|
| `SwitchItem`, `CheckboxItem`, `DropdownItem`, `SingleSelectItem`, `TextInputItem`, `NumberInputItem`, `JsonInputItem` | Editing values |
| `ButtonItem`, `ConfirmButtonItem` | Actions (the confirm version needs a second tap) |
| `PageLinkItem`, `ExpandableItem`, `Section`, `CollapsibleSection`, `SectionDivider`, `DeckCard` | Structure and sub-pages |
| `SegmentedPills`, `FilterPill`, `FilterPillRow`, `ActionPill`, `ActionPillRow`, `SearchField` | View switchers, filters and search |
| `BulletItem`, `InfoGroup`, `KeyValueItem`, `CodeBlock`, `TextItem` | Showing information |

- Close or navigate from a plugin with `LocalDeckState.current?.close()` or `?.navigateTo(title) { … }`.
- For setup work, implement `DeckPlugin` and override `onAttach(context)`.

### Customizing

```kotlin
DevDeck.install(
    application = this,
    plugins = myPlugins,
    style = DeckStyle(title = "QA menu", subtitle = "Staging build"),
    side = DeckSide.Start,                              // open from the left
    includeActivity = { it !is SplashActivity },        // where the drawer appears
)

DevDeck.isEnabled = false                               // hide everywhere, e.g. during a 3DS step
DevDeck.updateConfig { it.copy(plugins = it.plugins + cachePlugin) }
```

`DeckStyle` also sets the panel width and colors; `collapsibleSections = false` shows every section open, and `oneSectionAtATime = true` closes the others when one opens.

**Payment screens:** DevDeck skips Adyen, Primer, Netcetera 3DS, Cardinal Commerce, Stripe, Braintree, Checkout.com and PayPal activities automatically (`DeckExclusions.PAYMENT_SDKS`). Add your own with `excludedActivityPrefixes`, and set `DevDeck.isEnabled = false` during 3DS inside your own screens.

---

## Reference

| Module | Package | Contents |
|---|---|---|
| `devdeck` | `io.devdeck` | Drawer, plugins and building blocks |
| `devdeck-okhttp` | `io.devdeck.okhttp` | `DevDeckOkHttpInterceptor` (OkHttp is `compileOnly`) |
| `devdeck-timber` | `io.devdeck.timber` | `DevDeckTimberTree`, `TimberLogsPlugin` (Timber 4.7.1+ or 5.x, `compileOnly`) |
| `devdeck-appdistribution` | `io.devdeck.appdistribution` | `FirebaseAppDistributionSource` (Firebase App Distribution API is `compileOnly`) |

- **Requirements:** minSdk 23, compileSdk 36. Built with AGP 9.4, Kotlin 2.2 and Compose BOM 2026.05; debug builds of apps on an older Compose resolve to this BOM's versions.
- **Versions:** `MAJOR.MINOR.PATCH`, with pre-releases as `-alphaNN`, `-betaNN` and `-rcNN` (e.g. `1.2.0-beta01`), like AndroidX. Minor and patch releases don't break your code.
- **Stored data:** settings in the `devdeck_settings` SharedPreferences file, flags in `devdeck_flags`, crash logs in `files/devdeck/crashes/`, captures in `devdeck/` under the external files directory, and exported reports in `cache/devdeck-reports/` (newest 20 kept).
- **Manifest additions:** foreground service and notification permissions for screen recording, a `FileProvider` (`${applicationId}.devdeck.files`) and the recording service.
- **R8:** consumer rules are included, so minified internal builds need no extra rules.
- **Limits:** *Slow animations* affects View animations only. Logcat shows your app's process only.

## Security

- **Keep DevDeck out of store builds.** It shows network traffic, logs, preferences and files, and can change app state.
- **Secrets are masked** in the network inspector, but your own logs, logcat lines and preference values are shown as they are.
- **Nothing leaves the device on its own.** Data is only shared when a tester taps *Share* or sends a bug report.
- **Found a vulnerability?** Report it privately, as described in [SECURITY.md](SECURITY.md).

## Sample app

Run the `sample` configuration in Android Studio. [sample/README.md](sample/README.md) walks through each screen and what to try.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for local checks, pull requests and releases, and [CHANGELOG.md](CHANGELOG.md) for what changed in each version. Please follow the [code of conduct](CODE_OF_CONDUCT.md).

## License

DevDeck is released under the [MIT License](LICENSE).
