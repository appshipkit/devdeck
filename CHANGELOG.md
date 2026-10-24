# Changelog

All notable changes to DevDeck are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

Add entries under **Unreleased** in each pull request. The *Bump version* workflow turns that section into the next release.

## [Unreleased]

## [0.1.0] - 2026-10-24

### Added
- Drawer that opens with a two-finger swipe, a floating button, Ctrl + Shift + D or code; app-wide install or per-screen `DevDeckHost`.
- Plugins: endpoints, feature flags, custom logs, logcat, crash logs, network inspector, UI tools, performance, appearance, storage, app actions, device and info.
- `devdeck-okhttp` interceptor and `devdeck-timber` tree.
- Automatic exclusion of payment and 3DS SDK screens.
- Collapsible sections: tap a section header to expand or collapse it (`DeckStyle.collapsibleSections`), plus a `CollapsibleSection` component for your own plugins. Opening a section or row scrolls it into view when it would land off screen.
- Network simulator (`NetworkSimulatorPlugin`): slow, flaky or failing network (no connection, timeout, server error) for calls through `DevDeckOkHttpInterceptor`, with an on-screen badge and *Simulated* tags in the inspector.
- Memory simulator (`MemorySimulatorPlugin`): send memory warnings, hold extra heap, and kill the app when backgrounded to test state restore.
