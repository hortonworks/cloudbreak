#!/usr/bin/env bash

set -e
DOCKER_SSC_IMAGE=docker-private.infra.cloudera.com/cloudera_thirdparty/tufin/oasdiff:v1.10.27
INCOMPATIBLE_CHANGES=()

date
cd $INTEGCB_LOCATION
cd ..

echo -e "OpenAPI check\n"
mkdir -p ./apidefinitions
cp ../core/build/openapi/cb.json ./apidefinitions/cloudbreak.json
cp ../environment/build/openapi/environment.json ./apidefinitions/environment.json
cp ../freeipa/build/openapi/freeipa.json ./apidefinitions/freeipa.json
cp ../redbeams/build/openapi/redbeams.json ./apidefinitions/redbeams.json
cp ../datalake/build/openapi/datalake.json ./apidefinitions/datalake.json
cp ../autoscale/build/openapi/autoscale.json ./apidefinitions/autoscale.json
cp ../environment-remote/build/openapi/remoteenvironment.json ./apidefinitions/remoteenvironment.json
cp ../externalized-compute/build/openapi/externalizedcompute.json ./apidefinitions/externalizedcompute.json

verlte() {
    [  "$1" = "`echo -e "$1\n$2" | sort -V | head -n1`" ]
}

compatible() {
  local service=$1
  local previous_build=$2
  local compat_results
  local compat_exit_code
  echo Downloading ${service} ${previous_build} OpenAPI definition, if possible https://${service}-swagger.s3.${zone[$service]}.amazonaws.com/openapi-${previous_build}.json
  STATUSCODE=$(curl -kfSs --write-out "%{http_code}" https://${service}-swagger.s3.${zone[$service]}.amazonaws.com/openapi-${previous_build}.json -o ./apidefinitions/${service}-openapi-${previous_build}.json)
  if [ $STATUSCODE -ne 200 ]; then
    echo download failed probably no available openapi definition for the previous version
    rm ./apidefinitions/${service}-openapi-${previous_build}.json
    compat_exit_code=0
  else
    compat_results=$(docker run --rm -t \
      -v ${PWD}/apidefinitions:/apidefinitions:rw \
      "${DOCKER_SSC_IMAGE}" \
      "changelog" \
      "/apidefinitions/${service}-openapi-${previous_build}.json" \
      "/apidefinitions/${service}.json" \
      --color never \
      -o ERR)
    compat_exit_code=$?

    echo
    if [[ $compat_exit_code == 1 ]]; then
      echo "COMPATIBILITY BREAKS in ${service}"
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
PREVIOUS_MINOR_BUILD=$(curl "http://release.eng.cloudera.com/hwre-api/listbuilds?stack=CB&release=${PREVIOUS_MINOR_VERSION}" | jq -r '.latest_build_version')
PREVIOUS_BUILD=$(curl "http://release.eng.cloudera.com/hwre-api/listbuilds?stack=CB&release=$VERSION" | jq '.full_list_versions[0]' | tr -d '"')
Services="cloudbreak,freeipa,environment,datalake,redbeams,autoscale,remoteenvironment,externalizedcompute"
declare -A zone=( ["cloudbreak"]="eu-central-1" ["freeipa"]="us-east-2" ["environment"]="us-east-2" ["datalake"]="us-east-2" ["redbeams"]="us-east-2" ["autoscale"]="us-east-2" ["remoteenvironment"]="us-east-2" ["externalizedcompute"]="us-east-2")
Field_Separator=$IFS
IFS=,
set +e
echo "Target branch for OpenAPI check: ${CB_TARGET_BRANCH}"

for service in $Services; do
  if [ "${CB_TARGET_BRANCH}" != "master" ] && verlte 2.31.0-b118 $PREVIOUS_BUILD ; then
    if ! compatible "${service}" "${PREVIOUS_BUILD}"; then
      INCOMPATIBLE_CHANGES+=("$service:$PREVIOUS_BUILD")
    fi
  fi
  if verlte 2.31.0-b118 $PREVIOUS_MINOR_BUILD ; then
    echo $PREVIOUS_MINOR_BUILD
    if ! compatible "${service}" "${PREVIOUS_MINOR_BUILD}"; then
      INCOMPATIBLE_CHANGES+=("$service:$PREVIOUS_MINOR_BUILD")
    fi
  fi
done

NUM_INCOMPATIBLE_CHANGES=${#INCOMPATIBLE_CHANGES[@]}
if (( NUM_INCOMPATIBLE_CHANGES > 0 )); then
  # Report any incompatible changes detected.
  echo
  echo "Incompatible changes:"
  for incompat in "${INCOMPATIBLE_CHANGES[@]}"; do
    echo "- $incompat"
  done
  NUM_INCOMPATIBLE_CHANGES=1
fi

exit $NUM_INCOMPATIBLE_CHANGES
