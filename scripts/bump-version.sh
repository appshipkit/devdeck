#!/usr/bin/env bash
# Usage: scripts/bump-version.sh <fix|feature|major|auto|x.y.z>
#   fix     0.1.0 -> 0.1.1   (alias: patch)
#   feature 0.1.0 -> 0.2.0   (alias: minor)
#   major   0.1.0 -> 1.0.0
#   auto    picks one from the [Unreleased] section of CHANGELOG.md; never leaves 0.x
set -euo pipefail

cd "$(dirname "$0")/.."

current=$(grep '^VERSION_NAME=' gradle.properties | cut -d= -f2)
IFS=. read -r major minor patch <<< "${current%%-*}"

unreleased=$(awk '/^## \[Unreleased\]/ { found = 1; next } found && /^## \[/ { exit } found { print }' CHANGELOG.md)

kind="${1:-}"
if [ "$kind" = "auto" ]; then
  if grep -qiE '^### Removed|BREAKING' <<< "$unreleased"; then
    kind=major
  elif grep -qE '^### (Added|Changed)' <<< "$unreleased"; then
    kind=feature
  elif grep -qE '^### Fixed' <<< "$unreleased"; then
    kind=fix
  else
    echo "auto: no Added/Changed/Fixed/Removed entries under [Unreleased] in CHANGELOG.md" >&2
    exit 1
  fi
  if [ "$kind" = "major" ] && [ "$major" = "0" ]; then
    kind=feature
    echo "auto: breaking change while on 0.x, so this stays a feature release; run 'major' to release 1.0.0"
  fi
  echo "auto: CHANGELOG.md entries mean a ${kind} release"
fi

case "$kind" in
  fix | patch) next="${major}.${minor}.$((patch + 1))" ;;
  feature | minor) next="${major}.$((minor + 1)).0" ;;
  major) next="$((major + 1)).0.0" ;;
  [0-9]*.[0-9]*.[0-9]*) next="$kind" ;;
  *)
    echo "Usage: $0 <fix|feature|major|auto|x.y.z>" >&2
    exit 1
    ;;
esac

today=$(date -u +%Y-%m-%d)

perl -pi -e "s/^VERSION_NAME=.*/VERSION_NAME=${next}/" gradle.properties
perl -pi -e "s/(com\.github\.appshipkit\.devdeck:devdeck[a-z-]*):\Q${current}\E/\1:${next}/g" README.md
perl -pi -e "s/^## \[Unreleased\]\s*\$/## [Unreleased]\n\n## [${next}] - ${today}\n/" CHANGELOG.md

echo "${current} -> ${next}"
if [ -n "${GITHUB_OUTPUT:-}" ]; then echo "version=${next}" >> "$GITHUB_OUTPUT"; fi
