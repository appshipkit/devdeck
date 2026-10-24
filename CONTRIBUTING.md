# Contributing to DevDeck

## Local checks

Run these before opening a pull request. They are the same checks CI runs:

```bash
./gradlew ktlintFormat                 # fix code style
./gradlew ktlintCheck lintDebug        # code style + Android Lint
./gradlew testDebugUnitTest            # unit tests
./gradlew assembleDebug assembleRelease publishToMavenLocal
```

Lint warnings fail the build, so fix them rather than ignoring them. Dependency-version warnings are turned off because Dependabot handles updates.

## Pull requests

- Branch from `main` and open a PR against `main`.
- Add a line under **[Unreleased]** in `CHANGELOG.md`, under `### Added`, `### Changed`, `### Fixed` or `### Removed`, and write `BREAKING:` in front of anything that breaks existing code. The `auto` release type uses these headings. Use the `skip-changelog` label for changes users won't notice, like CI or docs.
- CI must pass: **Lint**, **Build and test** and **Changelog**.
- A code owner (see `.github/CODEOWNERS`) must approve.
- Merged PRs are added to the **Next release** milestone automatically.

## Versioning and releases

DevDeck uses [Semantic Versioning](https://semver.org/). The version lives in one place: `VERSION_NAME` in `gradle.properties`.

1. Run the **Bump version** workflow (Actions tab → *Bump version*) and pick the release type:

   | Release type | Example | Use for |
   |---|---|---|
   | `fix` | 0.1.0 → **0.1.1** | Bug fixes only |
   | `feature` | 0.1.0 → **0.2.0** | New features or behaviour changes |
   | `major` | 0.x → **1.0.0**, then 1.x → **2.0.0** | The first stable release, and later breaking changes: removed or renamed public API, changed defaults |
   | `auto` (default) | — | Picks one from the `[Unreleased]` entries in `CHANGELOG.md`: `### Removed` or the word `BREAKING` → major; `### Added` / `### Changed` → feature; only `### Fixed` → fix. While on 0.x, breaking changes stay a feature release, so 1.0.0 only happens when you choose `major` |

   It updates `VERSION_NAME`, moves the *Unreleased* changelog entries under the new version, updates the README's dependency lines, and opens a **Release vX.Y.Z** pull request.
2. Merge that pull request. The **Release** workflow then:
   - builds and tests,
   - creates the `vX.Y.Z` tag and a GitHub release with the changelog notes,
   - renames the **Next release** milestone to `vX.Y.Z` and closes it,
   - asks JitPack to build the tag.

You can also bump locally with `scripts/bump-version.sh <fix|feature|major|auto|x.y.z>` and open the PR yourself.

## One-time repository setup

Do this once after creating the GitHub repository:

1. **Settings → Branches → Add branch protection rule** for `main`:
   - Require a pull request before merging, with **Require review from Code Owners**.
   - Require status checks to pass: `Lint`, `Build and test`, `Changelog`.
   - Require branches to be up to date before merging.
2. **Settings → Actions → General → Workflow permissions:** select *Read and write permissions* and tick *Allow GitHub Actions to create and approve pull requests* (needed by *Bump version*).
3. **Optional: `RELEASE_TOKEN` secret.** Pull requests opened with the default Actions token don't trigger CI, so the release PR's required checks won't run. Add a fine-grained personal access token with *Contents* and *Pull requests* write access as a repository secret named `RELEASE_TOKEN`, and *Bump version* will use it. Without it, re-run the checks by pushing an empty commit to the release branch, or merge as an admin.
4. **Labels and milestone:** run the **Set up repository** workflow once (Actions tab). It creates the `bug`, `enhancement`, `dependencies` and `skip-changelog` labels used by the templates, Dependabot and CI, plus the **Next release** milestone.
