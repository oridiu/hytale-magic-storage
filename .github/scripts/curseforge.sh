#!/usr/bin/env bash
# Uploads the plugin jar to CurseForge, or with "check" only lists the game
# versions the API knows, so the endpoint and token can be tested safely.
#
# Usage: curseforge.sh check
#        curseforge.sh upload <jar> <tag>
#
# Env: CF_TOKEN, CF_PROJECT_ID (secrets), CF_API (base URL),
#      CF_RELEASE_TYPE (alpha|beta|release), CF_GAME_VERSION (optional name,
#      defaults to hytale_build from gradle.properties), CHANGELOG (optional).
set -euo pipefail

mode="${1:?mode missing}"

if [[ -z "${CF_TOKEN:-}" || -z "${CF_PROJECT_ID:-}" ]]; then
  echo "::notice::CURSEFORGE_TOKEN or CURSEFORGE_PROJECT_ID not set, skipping CurseForge."
  exit 0
fi

api="${CF_API%/}"
target="${CF_GAME_VERSION:-$(grep '^hytale_build=' gradle.properties | cut -d= -f2)}"

read -r status location < <(curl -sS -o versions.json -w '%{http_code} %{redirect_url}\n' \
  -H "X-Api-Token: ${CF_TOKEN}" "${api}/api/game/versions")
if [[ "$status" != 200 ]]; then
  echo "::error::GET ${api}/api/game/versions returned HTTP ${status}${location:+ (redirect to ${location})}. Check CURSEFORGE_API_URL and the token."
  head -c 2000 versions.json; echo
  exit 1
fi

echo "Game versions known to ${api}:"
jq -r '.[] | "  \(.id)\t\(.name)\t(slug: \(.slug // "-"))"' versions.json | head -n 100

version_id=$(jq -r --arg t "$target" \
  '[.[] | select(.name == $t or .slug == $t)][0].id // empty' versions.json)
if [[ -z "$version_id" ]]; then
  echo "::error::No game version named '${target}'. Set the repository variable CURSEFORGE_GAME_VERSION to one of the names above."
  exit 1
fi
echo "Using game version '${target}' (id ${version_id})."

if [[ "$mode" == check ]]; then
  echo "Check passed, nothing uploaded."
  exit 0
fi

jar="${2:?jar missing}"
tag="${3:?tag missing}"

jq -n \
  --arg changelog "${CHANGELOG:-Release ${tag}}" \
  --arg name "MagicStorage ${tag#v}" \
  --arg type "${CF_RELEASE_TYPE:-beta}" \
  --argjson version "$version_id" \
  '{changelog: $changelog, changelogType: "markdown", displayName: $name,
    releaseType: $type, gameVersions: [$version]}' > metadata.json
cat metadata.json

status=$(curl -sS -o response.json -w '%{http_code}' \
  -H "X-Api-Token: ${CF_TOKEN}" \
  -F "metadata=<metadata.json" \
  -F "file=@${jar}" \
  "${api}/api/projects/${CF_PROJECT_ID}/upload-file")
cat response.json; echo
if [[ "$status" -lt 200 || "$status" -ge 300 ]]; then
  echo "::error::Upload failed with HTTP ${status}."
  exit 1
fi
echo "Uploaded $(basename "$jar") to CurseForge project ${CF_PROJECT_ID}. It now waits for moderation."
