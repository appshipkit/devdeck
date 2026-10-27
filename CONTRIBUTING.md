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

DevDeck uses [Semantic Versioning](https://semver.org/) with AndroidX-style pre-releases: `1.2.0` for stable releases, and `1.2.0-alpha01`, `1.2.0-beta01`, `1.2.0-rc01` before that (two-digit counters, alpha → beta → rc → stable). The version lives in one place: `VERSION_NAME` in `gradle.properties`, and the release workflow rejects any other format.

1. Run the **Bump version** workflow (Actions tab → *Bump version*) and pick the release type:

   | Release type | Example | Use for |
   |---|---|---|
   | `fix` | 1.0.0 → **1.0.1** | Bug fixes only |
   | `feature` | 1.0.0 → **1.1.0** | New features or behaviour changes |
   | `major` | 0.x → **1.0.0**, then 1.x → **2.0.0** | The first stable release, and later breaking changes: removed or renamed public API, changed defaults |
   | `alpha` | 1.1.0 → **1.2.0-alpha01**, then → **1.2.0-alpha02** | Early previews; APIs may still change |
   | `beta` | 1.2.0-alpha03 → **1.2.0-beta01** | Feature complete; fixes only from here |
   | `rc` | 1.2.0-beta02 → **1.2.0-rc01** | Release candidate; ships unchanged unless a blocker turns up |
   | `stable` | 1.2.0-rc01 → **1.2.0** | Drops the pre-release suffix |
   | `auto` (default) | — | Picks one from the `[Unreleased]` entries in `CHANGELOG.md`: `### Removed` or the word `BREAKING` → major; `### Added` / `### Changed` → feature; only `### Fixed` → fix. While on 0.x, breaking changes stay a feature release, so 1.0.0 only happens when you choose `major` |

   It updates `VERSION_NAME`, moves the *Unreleased* changelog entries under the new version, and opens a **Release vX.Y.Z** pull request.
2. Merge that pull request. The **Release** workflow then:
   - builds and tests,
   - signs the four libraries and publishes them to Maven Central as `io.github.appshipkit:<module>:<version>` (they appear on [central.sonatype.com](https://central.sonatype.com/namespace/io.github.appshipkit) within about 30 minutes),
   - creates the `vX.Y.Z` tag and a GitHub release with the changelog notes, marked as a pre-release for alpha, beta and rc versions,
   - closes the `vX.Y.Z` milestone (stable releases only).

**Publishing an older release:** to put an already tagged version on Maven Central (for example one released before DevDeck moved there), create a branch from its tag, add the Maven Central setup, and run the **Publish to Maven Central** workflow (Actions tab) with that branch. The tag itself stays as it was.

You can also bump locally with `scripts/bump-version.sh <fix|feature|major|alpha|beta|rc|stable|auto|version>` and open the PR yourself. `./gradlew publishToMavenLocal` installs the libraries in `~/.m2` to try them in another project.

## One-time repository setup

Do this once after creating the GitHub repository:

1. **Settings → Branches → Add branch protection rule** for `main`:
   - Require a pull request before merging, with **Require review from Code Owners**.
   - Require status checks to pass: `Lint`, `Build and test`, `Changelog`.
   - Require branches to be up to date before merging.
2. **Settings → Actions → General → Workflow permissions:** select *Read and write permissions* and tick *Allow GitHub Actions to create and approve pull requests* (needed by *Bump version*).
3. **Optional: `RELEASE_TOKEN` secret.** Pull requests opened with the default Actions token don't trigger CI, so the release PR's required checks won't run. Add a fine-grained personal access token with *Contents* and *Pull requests* write access as a repository secret named `RELEASE_TOKEN`, and *Bump version* will use it. Without it, re-run the checks by pushing an empty commit to the release branch, or merge as an admin.
4. **Labels and milestone:** run the **Set up repository** workflow once (Actions tab). It creates the `bug`, `enhancement`, `dependencies` and `skip-changelog` labels used by the templates, Dependabot and CI, plus the **Next release** milestone.
5. **Maven Central publishing.** The release workflow needs these, set up once by an admin of the appshipkit organization:
   1. Sign in to the [Central Portal](https://central.sonatype.com) with GitHub, then *Namespaces → Add namespace* `io.github.appshipkit`. It shows a verification key; create a public repository with that exact name in the appshipkit organization, press *Verify*, then delete the repository.
   2. *Account → Generate user token*. Save the token's username and password.
   3. Create a signing key: `gpg --full-generate-key` (RSA 4096), publish the public key with `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`, and export the private key with `gpg --armor --export-secret-keys <KEY_ID>`.
   4. Add four repository secrets (*Settings → Secrets and variables → Actions*): `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` (the token), `SIGNING_KEY` (the exported private key, including the BEGIN/END lines) and `SIGNING_KEY_PASSWORD`.
