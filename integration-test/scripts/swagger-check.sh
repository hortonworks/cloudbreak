#!/usr/bin/env bash

set -e
DOCKER_SSC_IMAGE=docker-private.infra.cloudera.com/cloudera_base/swagger-spec-compatibility:1.2.4
INCOMPATIBLE_CHANGES=()

date
cd $INTEGCB_LOCATION
cd ..

echo -e "Swagger check\n"
mkdir -p ./apidefinitions
cp ../core/build/swagger/cb.json ./apidefinitions/cloudbreak.json
cp ../environment/build/swagger/environment.json ./apidefinitions/environment.json
cp ../freeipa/build/swagger/freeipa.json ./apidefinitions/freeipa.json
cp ../redbeams/build/swagger/redbeams.json ./apidefinitions/redbeams.json
cp ../datalake/build/swagger/datalake.json ./apidefinitions/datalake.json
cp ../autoscale/build/swagger/autoscale.json ./apidefinitions/autoscale.json


verlte() {
    [  "$1" = "`echo -e "$1\n$2" | sort -V | head -n1`" ]
}

compatible() {
  local service=$1
  local previous_build=$2
  local compat_results
  local compat_exit_code
  echo Downloading ${service} ${previous_build} swagger definition, if possible https://${service}-swagger.s3.${zone[$service]}.amazonaws.com/swagger-${previous_build}.json
  STATUSCODE=$(curl -kfSs --write-out "%{http_code}" https://${service}-swagger.s3.${zone[$service]}.amazonaws.com/swagger-${previous_build}.json -o ./apidefinitions/${service}-swagger-${previous_build}.json)
  if [ $STATUSCODE -ne 200 ]; then
    echo download failed $STATUSCODE
    rm ./apidefinitions/${service}-swagger-${previous_build}.json
    compat_exit_code=1
  else
    from_file=${PWD}/apidefinitions/${service}-swagger-${previous_build}.json
    docker_from_file=/from/$(basename "$from_file")
    to_file=${PWD}/apidefinitions/${service}.json
    docker_to_file=/to/$(basename "$to_file")
    compat_results=$(docker run --rm -v "${from_file}:${docker_from_file}" \
          -v "${to_file}:${docker_to_file}" \
          "${DOCKER_SSC_IMAGE}" \
          run "${docker_from_file}" "${docker_to_file}" \
          -r MIS-E001 \
          -r MIS-E002 \
          -r REQ-E001 \
          -r REQ-E002 \
          -r REQ-E003 \
          -r REQ-E004 \
          -r RES-E001 \
          -r RES-E002 2>&1)
    compat_exit_code=$?
    if (( compat_exit_code != 0 )); then
      echo
      echo "================ COMPATIBILITY BREAKS in ${service} ================"
      echo "$compat_results"
      echo "==============================================================================="
      echo
    else
      echo "change is compatible"
    fi
    return $compat_exit_code
  fi
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
PREVIOUS_MINOR_BUILD=$(curl "http://release.infra.cloudera.com/hwre-api/listbuilds?stack=CB&release=${PREVIOUS_MINOR_VERSION}" | jq -r '.latest_build_version')
PREVIOUS_BUILD=$(curl "http://release.infra.cloudera.com/hwre-api/listbuilds?stack=CB&release=$VERSION" | jq '.full_list_versions[0]' | tr -d '"')
Services="cloudbreak,freeipa,environment,datalake,redbeams"
declare -A zone=( ["cloudbreak"]="eu-central-1" ["environment"]="us-east-2" ["datalake"]="us-east-2" ["redbeams"]="us-east-2" ["freeipa"]="us-east-2")
Field_Separator=$IFS
IFS=,
set +e
echo "Target branch for swagger check: ${CB_TARGET_BRANCH}"

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