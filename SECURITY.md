# Security policy

## Reporting a vulnerability

Please don't open a public issue for security problems. Report them privately through **GitHub → Security → Report a vulnerability** on this repository.

Include the DevDeck version, what you found, and steps to reproduce it. You'll get a reply within a week, and a fix will be released as soon as it's ready.

## Supported versions

Fixes are made on the latest release. Please update to it before reporting.

## Scope

DevDeck is a debug tool for test builds. Exposing app data to the person holding the device is its purpose, not a vulnerability. Reports are welcome for:

- data leaving the device without a tester's action
- secrets shown despite redaction (`NetworkLogger.redactedHeaders` / `redactedFields`)
- other apps reaching DevDeck's components or files
- problems in the build and release workflows
