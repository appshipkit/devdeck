# DevDeck sample app

This app shows every part of the DevDeck library in use. Open the project in Android Studio and run the **`sample`** configuration on a device or emulator.

The sample shows the two ways to attach the drawer:

| Screen | How the drawer is attached | Where to look |
|---|---|---|
| **Main screen** (Compose) | `DevDeckHost(plugins = …)` wraps the screen's content | `ui/SampleScreen.kt` |
| **View-based screen** (plain Views, no Compose) | `DevDeck.install(...)` in `Application.onCreate` adds it to the activity automatically | `SampleApp.kt`, `LegacyViewActivity.kt` |

Both screens share one plugin list, so their drawers show the same menu.

## Project layout

```
sample/src/main/kotlin/io/devdeck/sample/
├── SampleApp.kt              Creates the flag store and plugin list, and installs DevDeck app-wide
├── MainActivity.kt           Hosts the Compose screen
├── LegacyViewActivity.kt     View-only screen; also the target of the devdeck://sample/legacy deep link
├── debugmenu/                Everything that defines what's inside the drawer
│   ├── SampleDeckStyle.kt    The drawer's title and subtitle, shared by both screens
│   ├── SamplePlugins.kt      The plugin list and network highlight rules: the whole debug menu in one place
│   ├── SampleFlags.kt        Feature-flag definitions (toggle, text, number, JSON)
│   ├── SampleEndpoints.kt    Endpoint list and a helper that reads the selected one
│   ├── SampleUpdateSource.kt A fake update feed that always offers v1.1.0
│   ├── ControlsDemoPlugin.kt Switch, checkbox, dropdown and radio components
│   └── TapCounterPlugin.kt   A custom plugin built with deckPlugin { }
├── network/
│   └── SampleApi.kt          OkHttp client with DevDeckOkHttpInterceptor and six named demo calls
└── ui/                       The sample's own screen, not part of the drawer
    ├── SampleScreen.kt       DevDeckHost around the main screen
    ├── DrawerOptions.kt      State behind the live drawer settings (gestures, button, side)
    ├── DrawerControls.kt     Switches that change those settings at runtime
    ├── CurrentValues.kt      Shows live flag and endpoint values read from the store
    ├── NetworkDemo.kt        Buttons that send the demo requests
    └── LoggingDemo.kt        Buttons that log through logcat, Timber and DeckLogger
```

## How the drawer is put together

### 1. The plugin list: `debugmenu/SamplePlugins.kt`

The whole menu is one list, rendered top to bottom. Plugins with the same `section` share a header when they sit next to each other.

The order follows a typical test session: set up the environment and test conditions first, then the logs you check most, then everyday actions and tools, with reference info at the bottom.

```kotlin
fun samplePlugins(context: Context, flagStore: FeatureFlagStore): List<DeckPlugin> = listOf(
    SampleEndpoints.plugin,
    FeatureFlagsPlugin(SampleFlags.all, flagStore),
    NetworkSimulatorPlugin(),
    MemorySimulatorPlugin(),
    NetworkLogsPlugin(highlights = networkHighlights),
    LogcatPlugin(),
    TimberLogsPlugin(),
    LogsPlugin(tags = listOf("General", "Analytics"), lifecycleLogs = true),
    BreadcrumbsPlugin(),
    CrashPlugin(testCrashButton = true),
    AppActionsPlugin(deepLinkSuggestions = listOf("devdeck://sample/legacy")),
    UpdatesPlugin(source = SampleUpdateSource, notify = true),
    UiToolsPlugin(),
    AppearancePlugin(languageTags = listOf("en", "es", "fr", "hi")),
    PerformancePlugin(),
    StoragePlugin(),
    DevicePlugin(),
    tapCounterPlugin,
    controlsDemoPlugin,
    InfoPlugin(buildInfo = { mapOf("Endpoint" to SampleEndpoints.current(context).url) }),
)
```

| Plugin | What you see in the drawer |
|---|---|
| `EndpointPlugin` | **Configuration 🔧 → Endpoint**: dropdown, with the selected URL underneath. `SampleEndpoints` creates the plugin once, and `SampleEndpoints.current(context)` reads the saved choice with `plugin.current(context)`. |
| `FeatureFlagsPlugin` | **Configuration 🔧 → Feature flags**: *Ready for Testing ✅* / *In Development ⚠️* sections, category pages, and *Bulk edit ✏️* (reset, import or export via clipboard). |
| `NetworkSimulatorPlugin` | **Simulate 🧪 → Network**: make *Network demo* requests slow or fail. |
| `MemorySimulatorPlugin` | **Simulate 🧪**: memory warnings, *Fill memory*, and *Kill app when backgrounded*. |
| `NetworkLogsPlugin` | **Logs 📚 → Network activity**: every request sent by `SampleApi`, with *GraphQL*, *Countries* and *Orders* highlights. |
| `LogcatPlugin` | **Logs 📚 → Logcat**: everything the sample's process writes to logcat, e.g. `SampleApi` responses and the *Logcat* demo buttons. |
| `TimberLogsPlugin` | **Logs 📚 → Timber logs**: every `Timber` call, grouped by tag. |
| `LogsPlugin` | **Logs 📚**: custom `DeckLogger` logs. General, Analytics and Lifecycle rows with entry counts; each opens its own page. |
| `BreadcrumbsPlugin` | **Logs 📚 → Breadcrumbs**: screens, taps, requests and app events. Open the View-based screen, tap its buttons and come back to see them named. |
| `CrashPlugin` | **Logs 📚 → Crash logs** plus a *Trigger test crash* button. |
| `AppActionsPlugin` | **App 🛠️**: deep-link launcher (suggests `devdeck://sample/legacy`), WebView debugging, restart, clear cache or data, kill. |
| `UpdatesPlugin` | **App 🛠️ → Test build updates**: `SampleUpdateSource` pretends v1.1.0 (10100) is out, so you can see the header chip, the dot on the bug button, release notes and a fake install. Real apps use `FirebaseAppDistributionSource`. |
| `UiToolsPlugin` | **User Interface 🎨**: slow animations, keyline overlay, screen capture tools. |
| `AppearancePlugin` | **User Interface 🎨**: dark mode override and app language (English, Spanish, French, Hindi). |
| `PerformancePlugin` | **System 📱**: FPS / jank / memory overlay and memory details. |
| `StoragePlugin` | **System 📱**: SharedPreferences browser/editor and storage usage. |
| `DevicePlugin` | **System 📱**: permissions with request/revoke actions, developer options. |
| `tapCounterPlugin` | **Custom 🧩**: an example of your own plugin. |
| `controlsDemoPlugin` | **Custom 🧩 → Controls**: the switch, checkbox, dropdown and radio components. |
| `InfoPlugin` | **Info ℹ️**: *App info*, *Device info*, *Device & app snapshot* (an image you can share or save, with the current endpoint as build info), bug report, and the version row. |

To experiment, remove a line, reorder the list, or pass options such as `UiToolsPlugin(screenCapture = false)` or `DevicePlugin(permissions = false)`.

### 2. Network inspector: `network/SampleApi.kt` + `SamplePlugins.kt`

`SampleApi` adds the interceptor once:

```kotlin
OkHttpClient.Builder()
    .addInterceptor(DevDeckOkHttpInterceptor())
    .build()
```

The *Network demo* buttons on the main screen send real requests:

| Button | `SampleApi` function | Request | What to look for in the inspector |
|---|---|---|---|
| Load profile | `loadProfile()` | `httpbin.org/anything/users/42/profile?include=…` with an `Authorization` header | Query parameters section; the `Authorization` value is redacted |
| Place order | `placeOrder()` | `httpbin.org/anything/orders` with a JSON body | Orange *Orders* highlight; request and response bodies pretty-printed |
| List countries | `listEuropeanCountries()` | `countries.trevorblades.com/graphql`, operation `Countries` | Shows as **Countries** instead of `/graphql`, with *GraphQL* and *Countries* tags |
| Missing product | `loadMissingProduct()` | `httpbin.org/catalog/products/999` (an unknown route, so the server returns 404) | Orange `404` status, listed under the *Errors* filter |
| Slow search | `searchProducts()` | `httpbin.org/anything/search?q=jordan&page=1`, delayed 2 s by a test interceptor | Duration around 2 s, shown as pending (…) until it finishes |
| Payments offline | `checkPaymentsHealth()` | `payments.devdeck.invalid/v1/health` (a host that doesn't exist) | `ERR` status and an *Error* section with the exception |

Below the buttons, a status line shows a spinner while requests are loading and then the last result with its time.

Tap a button several times, then switch the inspector to **By endpoint**. Each endpoint appears once, with its call count.

The highlight rules are in `SamplePlugins.kt`:

```kotlin
NetworkHighlight.graphQl()
NetworkHighlight.operation("Countries", Color(0xFF00897B), "Countries")
NetworkHighlight.urlContains("Orders", Color(0xFFFB8C00), "/orders")
```

### 3. Feature flags: `debugmenu/SampleFlags.kt`

Every flag type DevDeck supports appears here:

```kotlin
val newCheckout    = FeatureFlag.Toggle("new_checkout", default = false, section = READY, category = "checkout")
val bannerText     = FeatureFlag.Text("checkout_banner", default = "Free shipping today", …)
val maxCartTotal   = FeatureFlag.Number("max_cart_total", default = 2500.0, …)
val searchPageSize = FeatureFlag.Number("search_limit", default = 20.0, wholeNumber = true, range = 1.0..100.0, …)
val checkoutConfig = FeatureFlag.Json("checkout_config", default = """{"expressPay":true, …}""", …)
```

- `section` becomes the bold header on the *Feature flags* page, and `category` becomes a sub-page.
- Each type gets its own editor:
  - Toggle → switch
  - Text → text field
  - Number → numeric field with validation
  - Json → a highlighted preview with an **Edit** button. Edit opens a full-page editor with *Format*, *Minify*, *Copy*, *Paste*, live validation and *Apply*.
- Values are read with `flagStore[SampleFlags.newCheckout]`. `ui/CurrentValues.kt` does this, so drawer changes show on the main screen straight away.

### 4. A custom plugin: `debugmenu/TapCounterPlugin.kt`

```kotlin
val tapCounterPlugin = deckPlugin(id = "tap_counter", section = "Custom 🧩") {
    ExpandableItem("Custom component") {
        // any composable
    }
}
```

### 5. App-wide install: `SampleApp.kt`

```kotlin
DevDeck.install(
    application = this,
    plugins = plugins,
    style = sampleDeckStyle,
    includeActivity = { it is LegacyViewActivity },
)
```

`includeActivity` limits the app-wide drawer to `LegacyViewActivity`. `MainActivity` hosts its own drawer, and a screen using `DevDeckHost` takes over its activity anyway, so this filter is only for clarity. In a real app you'd usually allow every activity. Payment and 3DS SDK screens (Adyen, Primer, Netcetera, Cardinal, Stripe, Braintree, Checkout.com, PayPal) are skipped automatically through `excludedActivityPrefixes`, which defaults to `DeckExclusions.PAYMENT_SDKS`.

### 6. Per-screen host with live settings: `ui/SampleScreen.kt`

```kotlin
DevDeckHost(
    plugins = plugins,
    state = deckState,
    enabled = options.enabled,
    side = options.side,
    gestures = options.gestures,
    trigger = options.trigger,
    style = sampleDeckStyle,
) { /* screen content */ }
```

| Control on the main screen | Library setting |
|---|---|
| Drawer enabled | `enabled` |
| Two-finger swipe to open | `DeckGestures(twoFingerSwipe)` |
| Ctrl + Shift + D shortcut | `DeckGestures(keyboardShortcut = DeckShortcut())` / `null` |
| Floating button | `DeckTrigger.FloatingButton()` / `DeckTrigger.None` |
| Side | `DeckSide.Start` / `End` |
| Open drawer | `deckState.open()` |

## Things to try

1. **Open the drawer** by tapping the floating bug button, tapping *Open drawer*, swiping left with two fingers anywhere on the screen, or pressing **Ctrl + Shift + D**. The shortcut is the easiest way on an emulator, because a mouse can't do a two-finger swipe. Sections start collapsed; tap a header such as *Logs 📚* to open it.
2. **Network:** tap every *Network demo* button (some of them several times), then open *Logs → Network activity*.
   - Try the *Errors* and *GraphQL* filters and search for `countries`.
   - Switch to **By endpoint** to see each API grouped with its call count and failures, then open a group for its stats and every call.
   - Open a request, expand its headers and bodies, and tap *Copy cURL*.
3. **Feature flags:** in *Feature flags → checkout*, toggle *New checkout* and watch *Current values* change. Tap *Edit* on *Checkout config*, break the JSON to see the error, then *Format* and *Apply*.
4. **Crash logs:** tap *Trigger test crash* twice, reopen the app, and open *Logs → Crash logs*.
5. **Logs:** tap the *Logging demo* buttons, then compare *Logs → Logcat* (switch to *By tag*, filter *Error+*), *Logs → Timber logs*, and *Logs → Analytics logs*. Network demo calls also write `SampleApi` lines to logcat. `SampleApp` plants Timber's `DebugTree` too, so Timber lines appear in logcat as well.
6. **Performance:** in *System*, turn on *Frame rate & memory overlay* and scroll the main screen.
7. **Bad network:** in *Simulate → Network*:
   - Pick *Slow* and tap *Load profile*: the spinner runs for about 3 s before the result.
   - Pick *Failing → No connection*: every button fails at once with `UnknownHostException`. Switch back to *Normal* and they work again.
   - Try *Timeout* and *Server error 503*, a 50% *Failure rate*, or *Only URLs containing* `/orders`.
8. **Low memory:** in *Simulate*, open *Send memory warning* and tap *Running low*. Open *Fill memory*, tap *+64 MB* and watch *Java heap* grow, then *Release*. Turn on *Kill app when backgrounded*, press Home, and reopen the sample from recents.
9. **Storage:** change a flag, then open *System → Shared preferences → devdeck_flags* to see and edit the raw value.
10. **Deep link:** in *App → Open a deep link*, tap the `devdeck://sample/legacy` suggestion.
11. **Screen capture and bug report:** take a screenshot, then attach it in *Info → Generate a bug report* and share.
   - Open *Info → Device & app snapshot* and tap the share icon in the top-right corner of the card. The save icon puts the PNG in the capture gallery, where it can be attached to bug reports, and the copy icon copies the details as text.
12. **Two-finger swipe:** swipe left with two fingers over any control on either screen. The drawer opens, and single-finger taps and scrolls on the screen keep working as normal.
