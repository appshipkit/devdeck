# Changelog

All notable changes to DevDeck are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

Add entries under **Unreleased** in each pull request. The *Bump version* workflow turns that section into the next release.

## [Unreleased]

### Added
- MIT license, code of conduct, and a demo GIF in the README showing each feature's effect in the sample app.
- `NetworkLogger.maxTotalBodyChars` (default 12M characters) caps the bodies kept across all calls; the oldest bodies are dropped first, their calls stay listed.
- `AppActions.clearCacheInBackground()` clears the cache off the main thread.
- `TextInputItem`, `NumberInputItem` and `JsonInputItem` take an optional `subtitle`.
- While recording the screen, a small floating bar shows the time with pause/resume (Android 7+) and stop, so you don't have to reopen the drawer; the drawer header shows a *Recording* chip with a red stop button that stays in place while the chips scroll (tapping the chip opens the screen capture tools), and the notification has Pause/Resume too.
- A *System language & region* button in the User Interface section opens Android's language and region settings (`LocaleSettingsButton()`).
- The feature flags page has *Restart screen* and *Restart app* buttons, so flag changes that are read once per screen or process take effect.
- After switching endpoints, `EndpointPlugin` shows a *Restart app* button so the app picks up the new endpoint (unless `restartOnChange` already restarts it).
- Share buttons on the Logs and Network activity pages, and share-all / delete-all in the capture gallery.
- Consumer R8 rules for devdeck-okhttp, devdeck-timber and devdeck-appdistribution, so minified builds don't fail when OkHttp, Timber or Firebase isn't on the classpath.

### Changed
- The sample's fake update feed offers 1.1.0 now that the sample is 1.0.0.
- README install lines use a `LATEST_VERSION` placeholder that matches the JitPack badge, plus a version catalog example.
- Drawer pages slide in and out instead of switching instantly.
- The network inspector's clear button sits in the count row, like the other lists.
- The device & app snapshot shows share, save and copy as small icons on the card; they are not part of the shared image.
- The deep link launcher opens links from a send icon inside the field, leaving more room on small phones.
- The sample app is named DevDeck.
- Permissions open on their own page instead of expanding inside the System section.
- The update chip in the header reads *New version* and opens the App section at *Test build updates*.
- Drawer text is a little smaller, list rows use Material's 48dp minimum height, and section titles and dividers take less space, so more fits on small phones.
- The header's settings and close buttons sit closer together.
- *Device info* moved from the System section to the Info section, next to *App info*. `DevicePlugin(deviceInfo = …)` still compiles but no longer has an effect.
- The app version shows under the drawer subtitle (*Version 1.0.0 (10000)*) instead of as a chip.
- *Take a screenshot*, *Record a video*, *Open the gallery* and deep link suggestions use the standard button and page rows instead of bullet points.
- The floating button stays where you drag it, across screens, opening the drawer and app restarts.
- The Info section starts with *App info*: version, package, build type and flavor (from the app's `BuildConfig` when it exists), whether the build is debuggable or R8 obfuscated or shrunk, and target and min SDK. `AppInfo()` and the device & app snapshot show the same details.
- Toggle flags no longer show an *Overridden* subtitle.
- Clearing logs, crash logs, breadcrumbs, requests, console lines, captures or shared preferences, and resetting all feature flags, now ask for a second tap.
- Network request actions (*Copy URL*, *Copy cURL*, *Share*) and label rows wrap on narrow screens instead of overflowing.
- Category, level and status colors have dark-theme variants, and labels on colored fills pick a readable text color.
- Filter pills, action pills, suggestion rows and code block buttons have 48dp touch targets; expandable log rows announce their state to TalkBack.
- Bug report and log exports are written to the app's cache and only the newest 20 are kept, instead of piling up in external storage.
- Network redaction matches field names regardless of case, `_` or `-` (`accessToken`, `client-secret`), masks numbers and booleans too, and also hides `X-Auth-Token`, `X-Access-Token`, `X-CSRF-Token` and `X-XSRF-Token` headers by default.
- README states that DevDeck stays out of release builds only when added with `debugImplementation`.

### Fixed
- Search placeholders and the network count row stay on one line on small phones.
- The permissions list refreshes after a permission dialog or settings screen closes, and shows when a revoke only applies after the app restarts.
- Tap breadcrumbs no longer record what was typed into text fields, so passwords can't end up in crash files or bug reports.
- Taking a screenshot no longer crashes the app when the window has no surface or the disk is full, and two captures in the same second no longer overwrite each other.
- The OkHttp interceptor no longer holds back streaming text responses while capturing them, and skips bodies with no content type.
- Clearing the console no longer brings back lines that were waiting to be shown.
- Starting a screen recording again while one is running no longer stops it.
- Logcat timestamps get the right year after New Year.
- Lifecycle logs are no longer recorded twice after `DevDeck.updateConfig`.
- Opening display settings or signing out of Firebase App Distribution no longer crashes when it fails.
- Crash logs, captures, shared preferences, bug reports and cache clearing no longer read or write files on the main thread.
- Expanded log rows keep their state when new lines arrive.
- Dependabot no longer proposes Android Gradle Plugin major or minor updates, as the 1.0.0 entry promised.

## [1.0.0] - 2026-10-24

First stable release. The public API now follows semantic versioning: breaking changes only in a new major version.

### Changed
- Published from the [appshipkit](https://github.com/appshipkit) organization as `com.github.appshipkit.devdeck`.
- Dependabot only proposes updates that don't force apps onto a newer compileSdk or Kotlin version.

### Fixed
- Install lines use the `v`-prefixed version that JitPack publishes, e.g. `com.github.appshipkit.devdeck:devdeck:v1.0.0`.
- The simulated network timeout no longer ends up to 1 ms early.

## [0.1.0] - 2026-10-24

First release.

### Added
- **Drawer:** opens with a two-finger swipe, a floating button, Ctrl + Shift + D or code. Install it app-wide or per screen with `DevDeckHost`. Payment and 3DS SDK screens are skipped automatically.
- **Layout:** a header with app version, settings gear and status chips; each section is a collapsible card with an emoji badge. `DeckStyle` options for colours, `collapsibleSections` and `oneSectionAtATime`.
- **Configuration:** endpoint switcher and feature flags (toggle, text, number, JSON) with import, export and reset.
- **Simulate:** slow, flaky or failing network through `DevDeckOkHttpInterceptor`, and memory warnings, heap pressure and kill-on-background.
- **Logs:** network inspector (REST and GraphQL), logcat, Timber (`devdeck-timber`), custom logs, crash logs, and breadcrumbs of screens, taps, requests and app events, saved with every crash and bug report.
- **App:** deep-link launcher, WebView debugging, restart, clear cache or data, kill, and test build updates (`UpdatesPlugin`, with `devdeck-appdistribution` for Firebase App Distribution or your own `UpdateSource`).
- **User Interface:** slow animations, keyline overlay, screenshots, screen recording, dark mode and app language overrides.
- **System:** frame rate and memory overlay, memory details, shared preferences browser, storage usage, device info, permissions with request, revoke and settings actions, developer options.
- **Info:** device and app snapshot image, bug report and version.
- **Security:** passwords, tokens and API keys are masked in logged URLs and bodies, as well as auth headers. Components are not exported, and workflows use pinned actions with least-privilege permissions.
- **Building blocks for your own plugins:** switches, checkboxes, dropdowns, radio lists, text, number and JSON inputs, buttons, pills, filters, search, cards, collapsible sections, info lists with dividers and code blocks.
