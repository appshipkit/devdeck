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
- Card layout: header with app version and active-simulation chips, and each section as a card with an emoji badge. `DeckCard` wraps your own content in the same card.
- `CheckboxItem` and `DropdownItem`, plus iOS-style switches and custom radio buttons and checkboxes (`CompactToggle`, `DeckRadio`, `DeckCheckbox`). Endpoint, dark mode, app language and the network simulator use dropdowns.
- Pill-style filters: a sliding segmented control for *All / By tag* and *Timeline / By endpoint*, coloured level and highlight pills, and a rounded search field (`SegmentedPills`, `FilterPill`, `SearchField`). The network simulator's extra settings animate in and out. Memory warnings and *Fill memory* use compact one-tap pills (`ActionPill`), showing the five warnings that still matter (all levels remain in `MemoryWarning`).
- Fewer sections: Performance, Storage and Device are one *System 📱* section, and Appearance joins *User Interface 🎨*. Info rows are compact label/value lines. Permissions show readable names with Granted/Denied pills, a summary and a *Manage* shortcut to the app's settings. Code blocks (JSON flags, network bodies, stack traces) have a slim toolbar with expand/collapse and copy icons and fade out when collapsed.
- Collapsible sections: tap a section header to expand or collapse it (`DeckStyle.collapsibleSections`), plus a `CollapsibleSection` component for your own plugins. Opening a section or row scrolls it into view when it would land off screen.
- Network simulator (`NetworkSimulatorPlugin`): slow, flaky or failing network (no connection, timeout, server error) for calls through `DevDeckOkHttpInterceptor`, with an on-screen badge and *Simulated* tags in the inspector.
- Memory simulator (`MemorySimulatorPlugin`): send memory warnings, hold extra heap, and kill the app when backgrounded to test state restore.
