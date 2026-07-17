#!/usr/bin/env bash

# Single source of truth for all services checked by the OpenAPI compatibility gate.
# Format: "service:module:zone:openapi_source"
#   service        — logical name (S3 bucket prefix, oasdiff filenames)
#   module         — Gradle module directory
#   zone           — AWS region of the published baseline S3 bucket
#   openapi_source — path to the generated OpenAPI JSON (relative to repo root)
#
# To add a new service: add one line here and create <module>/openapi-breaking-allowlist.txt.
# All consumers (build-swagger.sh, openapi-check.sh, openapi-allowlist-prune.sh) source this file.

SERVICES=(
  "cloudbreak:core:eu-central-1:core/build/openapi/cb.json"
  "freeipa:freeipa:us-east-2:freeipa/build/openapi/freeipa.json"
  "environment:environment:us-east-2:environment/build/openapi/environment.json"
  "datalake:datalake:us-east-2:datalake/build/openapi/datalake.json"
  "redbeams:redbeams:us-east-2:redbeams/build/openapi/redbeams.json"
  "autoscale:autoscale:us-east-2:autoscale/build/openapi/autoscale.json"
  "remoteenvironment:environment-remote:us-east-2:environment-remote/build/openapi/remoteenvironment.json"
  "externalizedcompute:externalized-compute:us-east-2:externalized-compute/build/openapi/externalizedcompute.json"
)

# Parse the registry into associative arrays for use by sourcing scripts.
declare -A SERVICE_MODULE=()
declare -A SERVICE_ZONE=()
declare -A SERVICE_OPENAPI_SOURCE=()
SERVICE_NAMES=()

for entry in "${SERVICES[@]}"; do
  IFS=: read -r name module zone openapi_source <<< "${entry}"
  SERVICE_NAMES+=("${name}")
  SERVICE_MODULE["${name}"]="${module}"
  SERVICE_ZONE["${name}"]="${zone}"
  SERVICE_OPENAPI_SOURCE["${name}"]="${openapi_source}"
done

DOCKER_SSC_IMAGE=docker-private.infra.cloudera.com/cloudera_thirdparty/tufin/oasdiff:v1.10.27

verlte() {
  [ "$1" = "$(echo -e "$1\n$2" | sort -V | head -n1)" ]
}

# Resolve the newest build of a release line whose OpenAPI spec is actually published to S3.
# A build is registered in the release API before its spec is uploaded, so the bleeding-edge
# build 404s; full_list_versions is newest-first, so walk it and take the first published one
# (probe cloudbreak — all services upload together).
newest_published_build() {
  local release=$1
  local probe_budget=10
  local candidate_build candidate_status
  for candidate_build in $( ( curl -s "http://release.eng.cloudera.com/hwre-api/listbuilds?stack=CB&release=${release}" | jq -r '.full_list_versions[]' ) || true); do
    candidate_status=$(curl -kSs -o /dev/null --write-out "%{http_code}" "https://cloudbreak-swagger.s3.${SERVICE_ZONE["cloudbreak"]}.amazonaws.com/openapi-${candidate_build}.json" 2>/dev/null || true)
    if [ "${candidate_status}" = "200" ]; then
      echo "${candidate_build}"
      return 0
    fi
    echo "Skipping ${release} build ${candidate_build} as baseline — spec not published yet (HTTP ${candidate_status:-000})." >&2
    probe_budget=$((probe_budget - 1))
    if [ "${probe_budget}" -le 0 ]; then
      echo "Reached probe budget while resolving ${release} baseline." >&2
      return 0
    fi
  done
  return 0
}
