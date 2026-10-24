# Changelog

All notable changes to DevDeck are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

Add entries under **Unreleased** in each pull request. The *Bump version* workflow turns that section into the next release.

## [Unreleased]

### Added
- MIT license, code of conduct, and a demo GIF in the README.

### Changed
- README install lines use a `LATEST_VERSION` placeholder that matches the JitPack badge, plus a version catalog example.

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
