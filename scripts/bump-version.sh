#!/usr/bin/env bash
# Usage: scripts/bump-version.sh <fix|feature|major|alpha|beta|rc|stable|auto|version>
#
# Versions follow AndroidX style: MAJOR.MINOR.PATCH for stable releases, and a two-digit
# pre-release suffix otherwise: 1.2.0-alpha01, 1.2.0-beta02, 1.2.0-rc01.
#
#   fix     1.1.0 -> 1.1.1   (alias: patch)
#   feature 1.1.0 -> 1.2.0   (alias: minor)
#   major   1.1.0 -> 2.0.0
#   alpha   1.1.0 -> 1.2.0-alpha01, 1.2.0-alpha01 -> 1.2.0-alpha02
#   beta    1.2.0-alpha03 -> 1.2.0-beta01, 1.2.0-beta01 -> 1.2.0-beta02 (1.1.0 -> 1.2.0-beta01)
#   rc      1.2.0-beta02 -> 1.2.0-rc01, 1.2.0-rc01 -> 1.2.0-rc02 (1.1.0 -> 1.2.0-rc01)
#   stable  1.2.0-rc02 -> 1.2.0
#   auto    picks fix, feature or major from the [Unreleased] section of CHANGELOG.md (stable versions only)
#   version an exact version such as 2.0.0-alpha01
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION_PATTERN='^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)[0-9]{2})?$'

current=$(grep '^VERSION_NAME=' gradle.properties | cut -d= -f2)
if ! [[ "$current" =~ $VERSION_PATTERN ]]; then
  echo "VERSION_NAME=${current} in gradle.properties isn't X.Y.Z or X.Y.Z-(alpha|beta|rc)NN" >&2
  exit 1
fi
base="${current%%-*}"
IFS=. read -r major minor patch <<< "$base"
stage=""
stage_number=0
if [[ "$current" == *-* ]]; then
  suffix="${current#*-}"
  stage="${suffix%%[0-9]*}"
  stage_number=$((10#${suffix#"$stage"}))
fi

stage_rank() {
  case "$1" in
    alpha) echo 1 ;;
    beta) echo 2 ;;
    rc) echo 3 ;;
  esac
}

unreleased=$(awk '/^## \[Unreleased\]/ { found = 1; next } found && /^## \[/ { exit } found { print }' CHANGELOG.md)

kind="${1:-}"
if [ "$kind" = "auto" ]; then
  if [ -n "$stage" ]; then
    echo "auto: ${current} is a pre-release; choose alpha, beta, rc or stable" >&2
    exit 1
  fi
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
  alpha | beta | rc)
    if [ -z "$stage" ]; then
      # A new pre-release line starts from the next feature version.
      next="${major}.$((minor + 1)).0-${kind}01"
    elif [ "$kind" = "$stage" ]; then
      next=$(printf '%s-%s%02d' "$base" "$kind" $((stage_number + 1)))
    elif [ "$(stage_rank "$kind")" -gt "$(stage_rank "$stage")" ]; then
      next="${base}-${kind}01"
    else
      echo "${current} is already past ${kind}; choose $([ "$stage" = rc ] && echo "rc or stable" || echo "a later stage or stable")" >&2
      exit 1
    fi
    ;;
  stable)
    if [ -z "$stage" ]; then
      echo "${current} is already stable; choose fix, feature or major" >&2
      exit 1
    fi
    next="$base"
    ;;
  *)
    if [[ "$kind" =~ $VERSION_PATTERN ]]; then
      next="$kind"
    else
      echo "Usage: $0 <fix|feature|major|alpha|beta|rc|stable|auto|X.Y.Z[-(alpha|beta|rc)NN]>" >&2
      exit 1
    fi
    ;;
esac

today=$(date -u +%Y-%m-%d)

perl -pi -e "s/^VERSION_NAME=.*/VERSION_NAME=${next}/" gradle.properties
perl -pi -e "s/^## \[Unreleased\]\s*\$/## [Unreleased]\n\n## [${next}] - ${today}\n/" CHANGELOG.md

echo "${current} -> ${next}"
if [ -n "${GITHUB_OUTPUT:-}" ]; then echo "version=${next}" >> "$GITHUB_OUTPUT"; fi
