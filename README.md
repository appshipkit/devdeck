# DevDeck

[![CI](https://github.com/Abhishek3289/devdeck/actions/workflows/ci.yml/badge.svg)](https://github.com/Abhishek3289/devdeck/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/Abhishek3289/devdeck.svg)](https://jitpack.io/#Abhishek3289/devdeck)

A debug drawer for Android apps, built with Jetpack Compose. Open it with a two-finger swipe, a floating button, a keyboard shortcut or code, and get your team's debug tools in one place:

- switch endpoints and override feature flags
- inspect network calls (REST and GraphQL)
- read your own logs, logcat and Timber logs, plus crash reports
- take screenshots and screen recordings, share bug reports and device info
- browse storage, watch frame rate and memory, and more

Everything is a plugin: pick the ones you need or write your own in a few lines. It works on Compose and classic View screens, and it never ships in release builds.

---

## Setup

### 1. Add the dependencies

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { maven("https://jitpack.io") }
}

// app/build.gradle.kts
dependencies {
    debugImplementation("com.github.Abhishek3289.devdeck:devdeck:0.1.0")
    debugImplementation("com.github.Abhishek3289.devdeck:devdeck-okhttp:0.1.0")  // optional: network inspector (OkHttp / Apollo)
    debugImplementation("com.github.Abhishek3289.devdeck:devdeck-timber:0.1.0")  // optional: Timber logs
}
```

### 2. Install it

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DevDeck.install(this)
    }
}
```

That's it: every activity now has a drawer with the default plugins.

### 3. (Optional) Capture network calls

```kotlin
OkHttpClient.Builder()
    .addInterceptor(DevDeckOkHttpInterceptor())   // addInterceptor, not addNetworkInterceptor
    .build()
```

For Timber, add `TimberLogsPlugin()` to your plugins (see [Choosing plugins](#choosing-plugins)); it plants its tree for you.

### Keep it out of release builds

DevDeck is a `debugImplementation` dependency, so release code can't reference it. Put all DevDeck code in the **debug** source set and an empty version in the **release** source set:

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
| **Ctrl + Shift + D** (emulator or hardware keyboard) | On | `DeckGestures(keyboardShortcut = DeckShortcut(keyCode = KeyEvent.KEYCODE_F12, ctrl = false, shift = false))`, or `null` to turn off |
| Floating bug button (draggable) | On | `trigger = DeckTrigger.None` |
| Code | — | `DevDeck.open()`, `DevDeck.close()`, `DevDeck.toggle()` |

- DevDeck never covers your controls. The swipe is detected by watching touches without taking them, so taps, scrolls and system back keep working.
- **On an emulator,** a mouse can't do a two-finger swipe, so use Ctrl + Shift + D or the floating button.
- If your app uses two-finger horizontal pans (maps, canvases), turn the swipe off.

---

## Choosing plugins

`DevDeck.install(this)` uses `DeckPlugins.defaults()`. To choose your own, pass a list; plugins render top to bottom, grouped by section. Tap a section header to expand or collapse it; sections start collapsed, so the drawer opens as a short list. The defaults follow a test session: test conditions first, then logs, everyday actions and tools, with reference info at the bottom.

```kotlin
val flags = SharedPreferencesFlagStore(this)

DevDeck.install(
    application = this,
    plugins = listOf(
        EndpointPlugin(listOf(Endpoint("Production", PROD_URL), Endpoint("Staging", STAGING_URL)), restartOnChange = true),
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

| Plugin | Section | What you get | In defaults |
|---|---|---|---|
| `EndpointPlugin` | Configuration | Endpoint picker | — needs endpoints |
| `FeatureFlagsPlugin` | Configuration | Toggle, text, number and JSON flags with reset and import/export | — needs flags |
| `NetworkSimulatorPlugin` | Simulate | Slow or failing network for OkHttp calls | ✓ |
| `MemorySimulatorPlugin` | Simulate | Memory warnings, heap pressure, kill in background | ✓ |
| `NetworkLogsPlugin` | Logs | Network inspector | ✓ |
| `LogcatPlugin` | Logs | Live logcat for your app's process | ✓ |
| `TimberLogsPlugin` | Logs | Every Timber call (module `devdeck-timber`) | — |
| `LogsPlugin` | Logs | Your own `DeckLogger.log(...)` entries, one row per tag; optional lifecycle logs | ✓ |
| `CrashPlugin` | Logs | Saved crashes with stack traces | ✓ |
| `AppActionsPlugin` | App | Deep-link launcher, WebView debugging, restart, clear cache/data, kill | ✓ |
| `UiToolsPlugin` | User Interface | Slow animations, keyline overlay, screenshot, screen recording, gallery | ✓ |
| `AppearancePlugin` | Appearance | Dark mode (Android 12+) and app language (Android 13+) override | ✓ |
| `PerformancePlugin` | Performance | FPS / jank / memory overlay and memory details | ✓ |
| `StoragePlugin` | Storage | SharedPreferences browser/editor and storage usage | ✓ |
| `DevicePlugin` | Device | Device info, permissions, app info, developer options | ✓ |
| `InfoPlugin` | Info | Device & app snapshot image, bug report, version | ✓ |

Every plugin takes a `section` (or `null` for no header) and switches to hide individual rows, e.g. `UiToolsPlugin(screenCapture = false)`.

**Only one Compose screen?** Wrap it instead of installing app-wide:

```kotlin
DevDeckHost(plugins = myPlugins) { AppContent() }
```

You can use both: a screen with `DevDeckHost` takes over its activity, and the app-wide drawer steps aside there.

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

- Need two pickers (say API and CDN)? Give each a `key`: `EndpointPlugin(cdnEndpoints, title = "CDN", key = "cdn")`.
- Flag `section` becomes a header on the flags page and `category` a sub-page.
- JSON flags get a highlighted preview and a full-page editor with Format, Minify and validation.
- To use your own flag system (LaunchDarkly, Firebase, an in-house repository), implement `FeatureFlagStore`: `get`, `set`, `isOverridden`, `reset`.

### Network inspector

- *Timeline* or *By endpoint* view: calls are grouped by GraphQL operation, or by method + path, with call counts, failures and timings.
- Each call shows its overview, query params, headers and pretty-printed bodies, with Copy URL, Copy cURL and Share.
- GraphQL operation names are detected automatically; `Authorization` and cookie headers are redacted.
- Streaming responses (server-sent events) are passed through untouched.
- Highlight what matters:

```kotlin
NetworkLogsPlugin(highlights = listOf(
    NetworkHighlight.graphQl(),
    NetworkHighlight.operation("Checkout", Color.Red, "PlaceOrder"),
    NetworkHighlight.urlContains("Payments", Color.Blue, "/payments/"),
))
```

Not using OkHttp? Report calls yourself with `NetworkLogger.start(...)` and `complete(...)` / `fail(...)`.

### Bad network and low memory

**Simulate 🧪 → Network** changes how requests behave, so testers can check loading, error and retry states without touching the device's connection:

| Condition | What happens |
|---|---|
| Normal | Requests go through untouched |
| Slow | Each request waits an extra delay (default 3 s) and downloads at a limited speed (default 50 KB/s), then succeeds |
| Failing | Requests fail as *No connection* (`UnknownHostException`), *Timeout* (`SocketTimeoutException` after the client's read timeout) or *Server error 503*, without reaching the server |

- *Failure rate* below 100% makes a flaky network; *Only URLs containing* limits it to matching calls, e.g. `/checkout`.
- Switch back to *Normal* to check that the app recovers.
- The setting survives restarts, and a badge stays on screen while it's on. Affected calls are tagged *Simulated* in the network inspector.
- It works through `DevDeckOkHttpInterceptor` (Apollo/GraphQL included). Add it as an application interceptor with `addInterceptor`, before other interceptors. Turn it off for a client with `DevDeckOkHttpInterceptor(simulateNetwork = false)`, or drive it from code with `NetworkSimulator.simulation = NetworkSimulation(...)`.

The memory rows under **Simulate 🧪** reproduce what the app sees on a low-memory device (an app can't lower the device's real memory):

- *Send memory warning* calls `onTrimMemory(level)` or `onLowMemory()` on the application, its registered `ComponentCallbacks` and open activities, as Android does.
- *Fill memory* holds extra Java heap (16 MB, 64 MB, or up to 90% of the limit) until you release it.
- *Kill app when backgrounded* ends the process the next time the app goes to the background. Reopen it from recents to test restoring state after process death.

### Logs

| Source | How logs get in |
|---|---|
| `LogsPlugin` | You call `DeckLogger.log("Order placed", tag = "Analytics", payload = json)` |
| `LogcatPlugin` | Automatic: everything your app process writes to logcat, no permission needed |
| `TimberLogsPlugin` | Automatic: every `Timber` call, with exact tags and levels |

Logcat and Timber pages have level filters, search, an *All / By tag* view with counts, pause, share and clear.

<details>
<summary>Logcat noise filtering</summary>

Android logs some lines inside your process that aren't about your app. These are hidden by default:

- tags: `perf_hint`, `ashmem`, `EGL_emulation`, `eglCodecCommon`, `HostConnection`, `goldfish-address-space`, `Gralloc4`, `libEGL`, `nativeloader`, `ziparchive`, `ProfileInstaller`, `CompatibilityChangeReporter`, `ImeTracker`, `InsetsController`
- tags starting with `VRI[`, and messages starting with `setRequestedFrameRate` (Android 15+ frame-rate logs from Compose)

`Choreographer` and `OpenGLRenderer` stay visible because their warnings point at real jank. Android Studio runs also add a few `hiddenapi` / `Redefining intrinsic method` warnings from Studio's own agent; those are harmless.

```kotlin
LogcatPlugin(
    ignoredTags = LogcatNoise.PLATFORM_TAGS + "MyNoisySdk",
    ignoredTagPrefixes = LogcatNoise.PLATFORM_TAG_PREFIXES,
    ignoredMessagePrefixes = LogcatNoise.PLATFORM_MESSAGE_PREFIXES,
)
```

Pass `emptySet()` to show everything. The filters apply to new lines.
</details>

### Screen capture, bug reports and device info

- **Screenshot / screen recording / gallery:** under User Interface. Recording asks for Android's screen-capture permission and shows a Stop notification.
- **Device & app snapshot:** under Info. One image with app version, build type, SDKs, device, Android version, screen and locale, plus your `buildInfo`. Share it, save it, or copy it as text.
- **Bug report:** a description, your logs, recent logcat, device info and chosen captures, sent through the share sheet.

---

## Your own plugins

```kotlin
val cachePlugin = deckPlugin(id = "cache", section = "Storage 💾") {
    ButtonItem("Clear image cache", onClick = { imageLoader.memoryCache?.clear() })
    PageLinkItem("Database tables") { TableList() }
}
```

Use the same building blocks as the built-in plugins:

| Building blocks | Use for |
|---|---|
| `SwitchItem`, `TextInputItem`, `NumberInputItem`, `JsonInputItem`, `SingleSelectItem` | Editing values |
| `ButtonItem`, `ConfirmButtonItem` | Actions (the confirm version needs a second tap) |
| `PageLinkItem`, `ExpandableItem`, `Section`, `SectionDivider` | Structure and sub-pages |
| `BulletItem`, `KeyValueItem`, `CodeBlock`, `TextItem` | Showing information |

- Close or navigate the drawer from your plugin with `LocalDeckState.current?.close()` or `?.navigateTo(title) { … }`.
- For setup work, implement `DeckPlugin` and override `onAttach(context)`; it runs once per plugin `id` with the application context.

---

## Control and customize

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

`DeckStyle` also sets the panel width, colors and container color. Pass `collapsibleSections = false` to show every section open, with plain headers.

### Payment and 3DS screens

DevDeck skips payment SDK activities automatically, so it never sits on top of card entry or 3DS challenges: Adyen, Primer, Netcetera 3DS, Cardinal Commerce, Stripe, Braintree, Checkout.com and PayPal (`DeckExclusions.PAYMENT_SDKS`).

```kotlin
DevDeck.install(this, plugins, excludedActivityPrefixes = DeckExclusions.PAYMENT_SDKS + "com.mybank.sdk.")  // add your own
DevDeck.install(this, plugins, excludedActivityPrefixes = emptySet())                                      // turn off
```

3DS in the bank's app or a browser runs in another process, so DevDeck is never there. For 3DS inside your own screen, set `DevDeck.isEnabled = false` during that step.

---

## Reference

| Module | Package | Contents |
|---|---|---|
| `devdeck` | `io.devdeck` | Drawer, plugins and building blocks. Depends on Compose, AndroidX Activity and Core. |
| `devdeck-okhttp` | `io.devdeck.okhttp` | `DevDeckOkHttpInterceptor`. OkHttp is `compileOnly`, so your version is used. |
| `devdeck-timber` | `io.devdeck.timber` | `DevDeckTimberTree`, `TimberLogsPlugin`. Timber is `compileOnly` (4.7.1+, works with 5.x). |

- **Requirements:** minSdk 23, compileSdk 36. Built with AGP 9.4, Kotlin 2.2 and Compose BOM 2026.05. If your app uses an older Compose, debug builds resolve to this BOM's versions.
- **Manifest additions:** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION` and `POST_NOTIFICATIONS` for screen recording, a `FileProvider` (`${applicationId}.devdeck.files`) and the recording service. Debug builds only, if you use `debugImplementation`.
- **Limits:** *Slow animations* affects View animations only, because Compose follows the system animation scale. Logcat shows your app's process only. Some OEM builds hide debug-level logs.

## Sample app

Run the `sample` configuration in Android Studio. [sample/README.md](sample/README.md) walks through each file and shows what to try.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for local checks, pull requests and the release process, and [CHANGELOG.md](CHANGELOG.md) for what changed in each version.
