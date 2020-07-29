#!/usr/bin/env bash

set -ex

date
echo -e "\n\033[1;96m--- Install cbd\033[0m\n"
cd $INTEGCB_LOCATION
./cbd generate
cd ..

date
echo -e "\n\033[1;96m--- Create docker network\033[0m\n"
docker network create cbreak_default || true

env

date
echo -e "\n\033[1;96m--- Swagger check\033[0m\n"
cp ../core/build/swagger/cb.json ./apidefinitions/cloudbreak.json
cp ../environment/build/swagger/environment.json ./apidefinitions/environment.json
cp ../freeipa/build/swagger/freeipa.json ./apidefinitions/freeipa.json
cp ../redbeams/build/swagger/redbeams.json ./apidefinitions/redbeams.json
cp ../datalake/build/swagger/datalake.json ./apidefinitions/datalake.json
cp ../autoscale/build/swagger/autoscale.json ./apidefinitions/autoscale.json


verlte() {
    [  "$1" = "`echo -e "$1\n$2" | sort -V | head -n1`" ]
}

VERSION=$(echo $CB_VERSION | cut -f 1 -d '-')
MAJOR_VERSION=$(echo $VERSION | cut -f 1 -d '.')
MINOR_VERSION=$(echo $VERSION | cut -f 2 -d '.')
PATCH_VERSION=$(echo $VERSION | cut -f 3 -d '.')
PREVIOUS_MINOR_VERSION=$MAJOR_VERSION.$(expr $MINOR_VERSION - 1).$PATCH_VERSION
PREVIOUS_MINOR_BUILD=$(curl "http://release.infra.cloudera.com/hwre-api/listbuilds?stack=CB&release=${PREVIOUS_MINOR_VERSION}" | jq -r '.latest_build_version')
PREVIOUS_BUILD=$(curl "http://release.infra.cloudera.com/hwre-api/listbuilds?stack=CB&release=$VERSION" | jq '.full_list_versions[1]' | tr -d '"')
Services="cloudbreak,freeipa,environment,datalake,redbeams,autoscale"
declare -A zone=( ["cloudbreak"]="eu-central-1" ["environment"]="us-east-2" ["datalake"]="us-east-2" ["redbeams"]="us-east-2" ["autoscale"]="us-east-2" ["freeipa"]="us-east-2")
Field_Separator=$IFS
IFS=,
set +e
echo "Target branch for swagger check: ${CB_TARGET_BRANCH}"
for service in $Services; do
  if [ "${CB_TARGET_BRANCH}" != "master" ] && verlte 2.26.0-b50 $PREVIOUS_BUILD ; then
    echo Downloading ${service} ${PREVIOUS_BUILD} swagger definition, if possible
    STATUSCODE=$(curl -kfSs --write-out "%{http_code}" https://${service}-swagger.s3.${zone[$service]}.amazonaws.com/swagger-${PREVIOUS_BUILD}.json -o ./apidefinitions/${service}-swagger-${PREVIOUS_BUILD}.json)
    if [ $STATUSCODE -ne 200 ]; then
      echo download failed $STATUSCODE
      rm ./apidefinitions/${service}-swagger-${PREVIOUS_BUILD}.json
    fi
  fi
  if verlte 2.26.0-b50 $PREVIOUS_MINOR_BUILD ; then
    echo Downloading ${service} ${PREVIOUS_MINOR_BUILD} swagger definition, if possible
    STATUSCODE=$(curl -kfSs --write-out "%{http_code}" https://${service}-swagger.s3.${zone[$service]}.amazonaws.com/swagger-${PREVIOUS_MINOR_BUILD}.json -o ./apidefinitions/${service}-swagger-${PREVIOUS_MINOR_BUILD}.json)
    if [ $STATUSCODE -ne 200 ]; then
      echo download failed $STATUSCODE
      rm ./apidefinitions/${service}-swagger-${PREVIOUS_MINOR_BUILD}.json
    fi
  fi
done
set -e
IFS=$Field_Separator
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-diff | tee swagger-diff.out
grep "swagger diff finished succesfully" swagger-diff.out
swaggerdiffresult=$?

$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-validation | tee swagger-validation-result.out
grep "swagger validation finished succesfully" swagger-validation-result.out
swaggervalidationresult=$?

exit $swaggerdiffresult && $swaggervalidationresult