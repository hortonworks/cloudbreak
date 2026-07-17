#!/usr/bin/env bash

set -e

date
cd $INTEGCB_LOCATION
cd ..

source ./scripts/service-registry.sh

echo -e "OpenAPI check\n"
mkdir -p ./apidefinitions

for service in "${SERVICE_NAMES[@]}"; do
  cp "../${SERVICE_OPENAPI_SOURCE[$service]}" "./apidefinitions/${service}.json"
done

# Stage per-service breaking-change allowlists into the mounted dir.
# Each allowlist lives in the service module that owns the API (see openapi-breaking-allowlist/README.md);
# oasdiff --err-ignore suppresses exactly those, so intentional breaks are introduced ONCE on
# the branch where they should first ship and forward-merge up — never seeded into older lines.
for service in "${SERVICE_NAMES[@]}"; do
  allowlist="../${SERVICE_MODULE[$service]}/openapi-breaking-allowlist.txt"
  if [ -f "${allowlist}" ]; then
    cp "${allowlist}" "./apidefinitions/${service}-breaking-allowlist.txt"
  fi
done

compatible() {
  local service=$1
  local previous_build=$2
  local compat_results
  local compat_exit_code
  local baseline_url="https://${service}-swagger.s3.${SERVICE_ZONE[$service]}.amazonaws.com/openapi-${previous_build}.json"
  local baseline_file="./apidefinitions/${service}-openapi-${previous_build}.json"
  echo "Downloading ${service} ${previous_build} OpenAPI definition, if possible: ${baseline_url}"
  STATUSCODE=$(curl -kSs --write-out "%{http_code}" "${baseline_url}" -o "${baseline_file}")
  if [ "${STATUSCODE}" != "200" ]; then
    echo "download failed, probably no available OpenAPI definition for ${service} build ${previous_build} (HTTP ${STATUSCODE:-000}) — skipping this baseline"
    rm -f "${baseline_file}"
    return 0
  else
    local ignore_args=()
    if [ -f "./apidefinitions/${service}-breaking-allowlist.txt" ]; then
      echo "Applying reviewed breaking-change allowlist for ${service}"
      ignore_args=(--err-ignore "/apidefinitions/${service}-breaking-allowlist.txt")
    fi
    compat_results=$(docker run --rm -t \
      -v ${PWD}/apidefinitions:/apidefinitions:rw \
      "${DOCKER_SSC_IMAGE}" \
      "changelog" \
      "/apidefinitions/${service}-openapi-${previous_build}.json" \
      "/apidefinitions/${service}.json" \
      --color never \
      "${ignore_args[@]}" \
      -o ERR)
    compat_exit_code=$?

    echo
    if [[ $compat_exit_code == 1 ]]; then
      echo "COMPATIBILITY BREAKS in ${service}"
    elif [[ $compat_exit_code -ne 0 ]]; then
      echo "ERROR checking ${service} (oasdiff/docker exit ${compat_exit_code}) — treated as incompatible"
    else
      echo "CHANGE IS COMPATIBLE in ${service}"
    fi
    echo "==============================================================================="
    echo "$compat_results"
    echo "==============================================================================="
    echo
  fi
  return $compat_exit_code
}

if [[ ! $CB_VERSION =~ ^([0-9]+\.)?([0-9]+\.)?(\*|[0-9]+)(\-b[0-9]+)?$ ]]; then
  echo CB_VERSION \($CB_VERSION\) does not look like as a valid version number
  echo Exit with failure
  exit 1
fi

VERSION=$(echo $CB_VERSION | cut -f 1 -d '-')
echo Determine previous version number based on current version \(which is $VERSION from input $CB_VERSION\),
MAJOR_VERSION=$(echo $VERSION | cut -f 1 -d '.')
MINOR_VERSION=$(echo $VERSION | cut -f 2 -d '.')
PATCH_VERSION=$(echo $VERSION | cut -f 3 -d '.')
echo where major version number is: $MAJOR_VERSION, minor: $MINOR_VERSION, patch: $PATCH_VERSION
PREVIOUS_MINOR_VERSION=$MAJOR_VERSION.$(expr $MINOR_VERSION - 1).$PATCH_VERSION

PREVIOUS_MINOR_BUILD=$(newest_published_build "${PREVIOUS_MINOR_VERSION}")
if [ -z "${PREVIOUS_MINOR_BUILD}" ]; then
  echo "WARNING: no published baseline found for previous minor ${PREVIOUS_MINOR_VERSION}; previous-minor comparison will be skipped."
fi
PREVIOUS_BUILD=$(newest_published_build "${VERSION}")
if [ -z "${PREVIOUS_BUILD}" ]; then
  echo "WARNING: no published current-line baseline found for ${VERSION}; current-line comparison will be skipped."
fi

INCOMPATIBLE_CHANGES=()
set +e
echo "Target branch for OpenAPI check: ${CB_TARGET_BRANCH}"

for service in "${SERVICE_NAMES[@]}"; do
  # Any non-zero from compatible() counts as incompatible (fail-closed on oasdiff/docker
  # errors); a skipped/missing baseline returns 0 and is not counted.
  if [ "${CB_TARGET_BRANCH}" != "master" ] && verlte 2.31.0-b118 "${PREVIOUS_BUILD:-0.0.0}"; then
    if ! compatible "${service}" "${PREVIOUS_BUILD}"; then
      INCOMPATIBLE_CHANGES+=("$service:$PREVIOUS_BUILD")
    fi
  fi
  if verlte 2.31.0-b118 "${PREVIOUS_MINOR_BUILD:-0.0.0}"; then
    echo $PREVIOUS_MINOR_BUILD
    if ! compatible "${service}" "${PREVIOUS_MINOR_BUILD}"; then
      INCOMPATIBLE_CHANGES+=("$service:$PREVIOUS_MINOR_BUILD")
    fi
  fi
done

EXIT_CODE=0

if (( ${#INCOMPATIBLE_CHANGES[@]} > 0 )); then
  # Report any incompatible changes detected.
  echo
  echo "Incompatible changes:"
  for incompat in "${INCOMPATIBLE_CHANGES[@]}"; do
    echo "- $incompat"
  done
  EXIT_CODE=1
fi

exit $EXIT_CODE
