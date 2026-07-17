#!/usr/bin/env bash

# Prune stale entries from <module>/openapi-breaking-allowlist.txt.
#
# An entry is STALE when removing it does NOT reintroduce a breaking change against the
# same baselines the gate (openapi-check.sh) compares CB_TARGET_BRANCH against:
#   - master        -> previous minor line's latest published build only
#   - release line  -> own line's latest published build AND previous minor's latest build
# We reuse oasdiff's own matching (leave-one-out: run with every OTHER entry ignored and
# check whether this entry's break reappears) instead of parsing the changelog text.
#
# Prints removed entries to stdout as "<service>\t<entry>" and rewrites the .txt files in place.

set -euo pipefail

cd "${INTEGCB_LOCATION}"
cd ..

source ./scripts/service-registry.sh

mkdir -p ./apidefinitions

# Download <service> baseline spec for a build into apidefinitions; echo the local path, or nothing on failure.
download_baseline() {
  local service=$1 build=$2
  local file="./apidefinitions/${service}-openapi-${build}.json"
  local status
  status=$(curl -kSs --write-out "%{http_code}" "https://${service}-swagger.s3.${SERVICE_ZONE[$service]}.amazonaws.com/openapi-${build}.json" -o "${file}" 2>/dev/null || true)
  if [ "${status}" = "200" ]; then echo "${file}"; else rm -f "${file}"; fi
}

# Returns shell-true (0) when the entry must be KEPT: removing it either leaves a breaking
# change (oasdiff exit 1) or compatibility could not be verified (docker/oasdiff error, any
# other non-zero). Returns false (1) only when the diff is clean (oasdiff exit 0), i.e. the
# entry is safe to drop. Conservative: never proposes a removal we couldn't confirm.
oasdiff_breaks() {
  local baseline_file=$1 current_file=$2 ignore_file=$3 rc=0
  docker run --rm -t -v "${PWD}/apidefinitions:/apidefinitions:rw" "${DOCKER_SSC_IMAGE}" \
    changelog "/apidefinitions/$(basename "${baseline_file}")" "/apidefinitions/$(basename "${current_file}")" \
    --color never --err-ignore "/apidefinitions/$(basename "${ignore_file}")" -o ERR >/dev/null 2>&1 || rc=$?
  [ "${rc}" -ne 0 ]
}

# Resolve version + baselines (shared logic from service-registry.sh).
if [[ ! $CB_VERSION =~ ^([0-9]+\.)?([0-9]+\.)?(\*|[0-9]+)(\-b[0-9]+)?$ ]]; then
  echo "CB_VERSION (${CB_VERSION}) is not a valid version number" >&2; exit 1
fi
VERSION=$(echo "$CB_VERSION" | cut -f 1 -d '-')
MAJOR_VERSION=$(echo "$VERSION" | cut -f 1 -d '.')
MINOR_VERSION=$(echo "$VERSION" | cut -f 2 -d '.')
PATCH_VERSION=$(echo "$VERSION" | cut -f 3 -d '.')
PREVIOUS_MINOR_VERSION=$MAJOR_VERSION.$(expr $MINOR_VERSION - 1).$PATCH_VERSION
PREVIOUS_MINOR_BUILD=$(newest_published_build "${PREVIOUS_MINOR_VERSION}")
PREVIOUS_BUILD=$(newest_published_build "${VERSION}")

for service in "${SERVICE_NAMES[@]}"; do
  allowlist="../${SERVICE_MODULE[$service]}/openapi-breaking-allowlist.txt"
  [ -f "${allowlist}" ] || continue

  # Non-comment, non-blank entries (the actual oasdiff messages).
  mapfile -t entries < <(grep -vE '^\s*(#|$)' "${allowlist}" || true)
  [ ${#entries[@]} -eq 0 ] && continue

  # Copy this service's current spec only when we actually need it.
  current="./apidefinitions/${service}.json"
  cp "../${SERVICE_OPENAPI_SOURCE[$service]}" "${current}"

  # Collect the baselines that apply to this branch (same as the gate).
  baselines=()
  if [ "${CB_TARGET_BRANCH}" != "master" ] && verlte 2.31.0-b118 "${PREVIOUS_BUILD:-0.0.0}"; then
    b=$(download_baseline "${service}" "${PREVIOUS_BUILD}"); [ -n "${b}" ] && baselines+=("${b}")
  fi
  if verlte 2.31.0-b118 "${PREVIOUS_MINOR_BUILD:-0.0.0}"; then
    b=$(download_baseline "${service}" "${PREVIOUS_MINOR_BUILD}"); [ -n "${b}" ] && baselines+=("${b}")
  fi
  # No usable baseline -> cannot prove anything stale; leave the file untouched.
  [ ${#baselines[@]} -eq 0 ] && continue

  removable=()
  # 'kept' shrinks as we confirm removals. Because oasdiff matches by case-insensitive
  # SUBSTRING, two entries can cover the same message (duplicate, or broad-vs-narrow). We only
  # drop an entry when the entries that CURRENTLY remain still suppress the break without it, so
  # a jointly-needed pair never gets fully removed (one of them stays behind).
  kept=("${entries[@]}")
  for entry in "${entries[@]}"; do
    # trial = kept minus the FIRST occurrence of this entry.
    trial="./apidefinitions/${service}-loo.txt"
    : > "${trial}"
    dropped_one=false
    new_kept=()
    for kept_entry in ${kept[@]+"${kept[@]}"}; do
      if [ "${dropped_one}" = false ] && [ "${kept_entry}" = "${entry}" ]; then
        dropped_one=true
        continue
      fi
      new_kept+=("${kept_entry}")
      printf '%s\n' "${kept_entry}" >> "${trial}"
    done
    # Already removed as an earlier duplicate — nothing left to test.
    [ "${dropped_one}" = false ] && continue
    still_needed=false
    for baseline in "${baselines[@]}"; do
      if oasdiff_breaks "${baseline}" "${current}" "${trial}"; then
        still_needed=true; break
      fi
    done
    if [ "${still_needed}" = false ]; then
      removable+=("${entry}")
      kept=(${new_kept[@]+"${new_kept[@]}"})
      printf '%s\t%s\n' "${service}" "${entry}"
    fi
  done

  # Rewrite from the original file, preserving comments/blank lines and dropping exactly the
  # pruned entry lines. Duplicates are handled by count: we keep as many copies of each entry
  # as remain in 'kept', so removing one of two identical lines leaves the other intact.
  if [ ${#removable[@]} -gt 0 ]; then
    declare -A keep_count=()
    for kept_entry in ${kept[@]+"${kept[@]}"}; do
      keep_count["${kept_entry}"]=$(( ${keep_count["${kept_entry}"]:-0} + 1 ))
    done
    tmp="${allowlist}.tmp"
    : > "${tmp}"
    while IFS= read -r line || [ -n "${line}" ]; do
      if [[ "${line}" =~ ^[[:space:]]*(#|$) ]]; then
        printf '%s\n' "${line}" >> "${tmp}"
      elif [ "${keep_count["${line}"]:-0}" -gt 0 ]; then
        printf '%s\n' "${line}" >> "${tmp}"
        keep_count["${line}"]=$(( keep_count["${line}"] - 1 ))
      fi
    done < "${allowlist}"
    mv "${tmp}" "${allowlist}"
    unset keep_count
  fi
done
