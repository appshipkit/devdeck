# Contributing to DevDeck

## Local checks

Run these before opening a pull request. They are the same checks CI runs:

```bash
./gradlew ktlintFormat                         # fix code style
./gradlew ktlintCheck lintDebug lintRelease    # code style + Android Lint
./gradlew testDebugUnitTest                    # unit tests
./gradlew apiCheck                             # public API unchanged (see below)
./gradlew assembleDebug assembleRelease publishToMavenLocal
./gradlew :sample:assembleQa                   # minified sample, catches R8 problems
```

Lint warnings fail the build, so fix them rather than ignoring them. Dependency-version warnings are turned off because Dependabot handles updates.

The build needs JDK 17. Gradle picks up an installed JDK 17 for its daemon (see `gradle/gradle-daemon-jvm.properties`) and downloads one only if none is found.

## Public API and binary compatibility

Each published module records its public API in `<module>/api/<module>.api`. `./gradlew apiCheck` (run by CI, the release workflow and `./gradlew check`) fails when the compiled release classes no longer match that file.

- **Adding API** (a new public class, function or property) is fine in a feature release. Run `./gradlew apiDump` and commit the updated `.api` files with your change.
- **Removing or changing API** (renaming, changing a signature, making something `internal`) breaks apps built against the previous version. That needs a major release and a `BREAKING:` changelog entry, so avoid it where an addition would do. Adding a parameter to an existing public function or constructor changes its JVM signature too, even with a default value: keep the old signature as an overload (it can be `@Deprecated(level = DeprecationLevel.HIDDEN)`).
- Mark anything users shouldn't call `internal`, so it stays out of the dump.

Review `.api` diffs like code: every line is a promise to library users.

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
   | `fix` | 1.0.0 → **1.0.1** | Bug fixes only |
   | `feature` | 1.0.0 → **1.1.0** | New features or behaviour changes |
   | `major` | 0.x → **1.0.0**, then 1.x → **2.0.0** | The first stable release, and later breaking changes: removed or renamed public API, changed defaults |
   | `auto` (default) | — | Picks one from the `[Unreleased]` entries in `CHANGELOG.md`: `### Removed` or the word `BREAKING` → major; `### Added` / `### Changed` → feature; only `### Fixed` → fix. While on 0.x, breaking changes stay a feature release, so 1.0.0 only happens when you choose `major` |

   It updates `VERSION_NAME`, moves the *Unreleased* changelog entries under the new version, and opens a **Release vX.Y.Z** pull request.
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
