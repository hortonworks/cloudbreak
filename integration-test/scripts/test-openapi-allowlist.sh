#!/usr/bin/env bash

# End-to-end test for the OpenAPI breaking-change allowlist feature.
# Exercises: detection → allowlist suppression → stale entry pruning.
# Requires: docker, network access to S3 (for baseline download).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/service-registry.sh"

WORKDIR=$(mktemp -d)
trap 'rm -rf "${WORKDIR}"' EXIT

PASS=0
FAIL=0

assert_eq() {
  local label=$1 expected=$2 actual=$3
  if [ "${expected}" = "${actual}" ]; then
    echo "  PASS: ${label}"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: ${label} (expected=${expected}, actual=${actual})"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== OpenAPI allowlist end-to-end test ==="
echo "Working directory: ${WORKDIR}"
echo ""

# --- Setup: download a real baseline from S3 ---
echo "--- Setup: resolving baseline ---"
BASELINE_BUILD=$(newest_published_build "2.111.0")
if [ -z "${BASELINE_BUILD}" ]; then
  echo "ERROR: could not resolve a published baseline for 2.111.0. Need network + S3 access."
  exit 1
fi
echo "Using baseline: ${BASELINE_BUILD}"

SERVICE="cloudbreak"
BASELINE_URL="https://${SERVICE}-swagger.s3.${SERVICE_ZONE[$SERVICE]}.amazonaws.com/openapi-${BASELINE_BUILD}.json"
BASELINE_FILE="${WORKDIR}/baseline.json"
echo "Downloading: ${BASELINE_URL}"
curl -kSs "${BASELINE_URL}" -o "${BASELINE_FILE}"
echo "Baseline size: $(wc -c < "${BASELINE_FILE}") bytes"
echo ""

# --- Setup: create a "current" spec with a breaking change ---
# Remove an endpoint entirely — oasdiff reports this as a breaking change.
REMOVED_PATH=$(python3 -c "
import json, sys
with open('${BASELINE_FILE}') as f:
    spec = json.load(f)
paths = sorted(spec['paths'].keys())
# Pick a GET endpoint to remove
for p in paths:
    if 'get' in spec['paths'][p]:
        print(p)
        sys.exit(0)
sys.exit(1)
")
echo "Breaking change: removing endpoint GET ${REMOVED_PATH}"

CURRENT_FILE="${WORKDIR}/current.json"
python3 -c "
import json
with open('${BASELINE_FILE}') as f:
    spec = json.load(f)
del spec['paths']['${REMOVED_PATH}']
with open('${CURRENT_FILE}', 'w') as f:
    json.dump(spec, f)
"
echo ""

# --- Test 1: breaking change is detected (no allowlist) ---
echo "--- Test 1: Breaking change detection (should fail) ---"
rc=0
docker run --rm -t \
  -v "${WORKDIR}:/specs:rw" \
  "${DOCKER_SSC_IMAGE}" \
  changelog /specs/baseline.json /specs/current.json \
  --color never -o ERR > "${WORKDIR}/oasdiff-output.txt" 2>&1 || rc=$?

assert_eq "oasdiff exits non-zero (breaking change detected)" "1" "${rc}"

# Capture the full breaking-change message for the allowlist.
# oasdiff --err-ignore matches by substring against the two-line block joined as one line:
#   "in API <METHOD> <path> <description>"
ESCAPED_PATH=$(echo "${REMOVED_PATH}" | sed 's/[/]/\\&/g')
BREAK_MESSAGE=$(awk '/in API.*'"${ESCAPED_PATH}"'/{prefix=$0; getline; gsub(/^[[:space:]]+/,"",$0); print prefix " " $0; exit}' "${WORKDIR}/oasdiff-output.txt")
echo "  Captured break message: ${BREAK_MESSAGE}"
echo ""

# --- Test 2: allowlist suppresses the breaking change ---
echo "--- Test 2: Allowlist suppression (should pass) ---"
ALLOWLIST_FILE="${WORKDIR}/allowlist.txt"
echo "${BREAK_MESSAGE}" > "${ALLOWLIST_FILE}"
cp "${ALLOWLIST_FILE}" "${WORKDIR}/specs-allowlist.txt"

rc=0
docker run --rm -t \
  -v "${WORKDIR}:/specs:rw" \
  "${DOCKER_SSC_IMAGE}" \
  changelog /specs/baseline.json /specs/current.json \
  --color never --err-ignore /specs/specs-allowlist.txt -o ERR > "${WORKDIR}/oasdiff-output2.txt" 2>&1 || rc=$?

assert_eq "oasdiff exits zero (allowlist suppresses break)" "0" "${rc}"
echo ""

# --- Test 3: stale entry detection (break reverted, allowlist remains) ---
echo "--- Test 3: Stale entry pruning (break reverted, allowlist kept) ---"
# Use the baseline as "current" — no actual break exists anymore
CURRENT_NO_BREAK="${WORKDIR}/current-no-break.json"
cp "${BASELINE_FILE}" "${CURRENT_NO_BREAK}"

# Run oasdiff with the allowlist — should pass (no break to find)
rc=0
docker run --rm -t \
  -v "${WORKDIR}:/specs:rw" \
  "${DOCKER_SSC_IMAGE}" \
  changelog /specs/baseline.json /specs/current-no-break.json \
  --color never --err-ignore /specs/specs-allowlist.txt -o ERR > /dev/null 2>&1 || rc=$?

assert_eq "oasdiff passes with stale allowlist (no break exists)" "0" "${rc}"

# Now test leave-one-out: run WITHOUT the allowlist — should also pass (proving entry is stale)
rc=0
docker run --rm -t \
  -v "${WORKDIR}:/specs:rw" \
  "${DOCKER_SSC_IMAGE}" \
  changelog /specs/baseline.json /specs/current-no-break.json \
  --color never -o ERR > /dev/null 2>&1 || rc=$?

assert_eq "oasdiff passes WITHOUT allowlist (entry is stale)" "0" "${rc}"
echo ""

# --- Test 4: non-stale entry is NOT pruned ---
echo "--- Test 4: Non-stale entry preserved (break still present) ---"
# With the mutated spec (break exists), removing the allowlist should fail
rc=0
docker run --rm -t \
  -v "${WORKDIR}:/specs:rw" \
  "${DOCKER_SSC_IMAGE}" \
  changelog /specs/baseline.json /specs/current.json \
  --color never -o ERR > /dev/null 2>&1 || rc=$?

assert_eq "oasdiff fails WITHOUT allowlist (entry is still needed)" "1" "${rc}"

# With the allowlist, it should pass
rc=0
docker run --rm -t \
  -v "${WORKDIR}:/specs:rw" \
  "${DOCKER_SSC_IMAGE}" \
  changelog /specs/baseline.json /specs/current.json \
  --color never --err-ignore /specs/specs-allowlist.txt -o ERR > /dev/null 2>&1 || rc=$?

assert_eq "oasdiff passes WITH allowlist (entry suppresses the break)" "0" "${rc}"
echo ""

# --- Summary ---
echo "=== Results: ${PASS} passed, ${FAIL} failed ==="
if [ "${FAIL}" -gt 0 ]; then
  exit 1
fi
