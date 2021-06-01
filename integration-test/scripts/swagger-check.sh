#!/usr/bin/env bash

set -e

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

echo "Checking if there are any API changes between this and the target branch"
git checkout $ghprbSourceBranch
git reset --hard ${ghprbActualCommit}

# list of the non-API modules
declare -a nonApiModules=("cloud-reactor-api"
  "common-handlebar"
  "datalake"
  "environment-log"
  "idbmms-connector"
  "redbeams-log"
  "structuredevent-service-legacy"
  "audit-connector"
  "ccm-connector"
  "cloud-reactor"
  "docker-autoscale"
  "environment"
  "integration-test"
  "redbeams-model"
  "auth-connector"
  "client-cm"
  "cloud-template"
  "docker-cloudbreak"
  "flow-api"
  "mock-thunderhead"
  "redbeams"
  "suites_log"
  "auth-internal-api"
  "cloud-api"
  "cloud-yarn"
  "config"
  "docker-datalake"
  "flow"
  "mock"
  "scripts"
  "template-manager-blueprint"
  "auth-internal"
  "cloud-aws-cloudformation"
  "cloudbreak-log"
  "docker-environment"
  "notification-sender"
  "sdx-log"
  "template-manager-cmtemplate"
  "authorization-common-api"
  "cloud-azure"
  "cloudera"
  "core-model"
  "docker-freeipa"
  "freeipa-client"
  "orchestrator-api"
  "secret-engine"
  "template-manager-core"
  "authorization-common"
  "cloud-common"
  "cluster-api"
  "core"
  "docker-redbeams"
  "freeipa-log"
  "orchestrator-salt"
  "status-checker"
  "template-manager-recipe"
  "cloud-gcp"
  "cluster-cm"
  "docs"
  "freeipa"
  "orchestrator-yarn"
  "template-manager-tag"
  "autoscale-log"
  "cloud-mock"
  "cluster-dns-connector"
  "datalake-dr-connector"
  "gradle"
  "performance-test"
  "usage-collection"
  "autoscale"
  "cloud-openstack"
  "cluster-proxy"
  "datalake-log"
  "environment-common"
  "grpc-common"
  "structuredevent-service-cdp"
  "workspace"
  "cloud-aws-common"
  "cloud-aws-native"
)

# list of the changed files
declare -a changedFiles=(
  $(git diff --name-only "origin/${CB_TARGET_BRANCH}")
)

# collector array for those file changes that are not in an API module
nonApiFileChanges=()

# iterating over all the non-API modules
for nonApiModule in "${nonApiModules[@]}"; do
  echo "Currently checking nonApiModule $nonApiModule for file changes:"
  # iterating over all the changed files
  for changedFile in "${changedFiles[@]}"; do
    # extracting the module name of the changed file
    moduleOfChangedFile=$(echo "$changedFile" | tr "/" " " | awk '{print $1}')
    if [[ "$moduleOfChangedFile" == "$nonApiModule" ]]; then
      # if the module name of the changed file is equal to the non-API modules name
      # then we add this file to the collector array
      nonApiFileChanges+=("$changedFile")
      echo "Non-API file change found"
      echo "$changedFile"
    fi
  done
  echo "--------"
done

echo "length of nonApiFileChanges" ${#nonApiFileChanges[@]}
echo "length of changedFiles" ${#changedFiles[@]}
# if the size of the non-API change collector array is equal with the number of changed files
# then all the changed files are in non-API modules
# and swagger check can be skipped
if [[ ${#nonApiFileChanges[@]} -eq ${#changedFiles[@]} ]]; then
  echo "All the changes are in non-API modules so swagger check can be skipped"
  exit 0
fi

for service in $Services; do
  if [ "${CB_TARGET_BRANCH}" != "master" ] && verlte 2.31.0-b118 $PREVIOUS_BUILD ; then
    echo Downloading ${service} ${PREVIOUS_BUILD} swagger definition, if possible
    STATUSCODE=$(curl -kfSs --write-out "%{http_code}" https://${service}-swagger.s3.${zone[$service]}.amazonaws.com/swagger-${PREVIOUS_BUILD}.json -o ./apidefinitions/${service}-swagger-${PREVIOUS_BUILD}.json)
    if [ $STATUSCODE -ne 200 ]; then
      echo download failed $STATUSCODE
      rm ./apidefinitions/${service}-swagger-${PREVIOUS_BUILD}.json
    fi
  fi
  if verlte 2.31.0-b118 $PREVIOUS_MINOR_BUILD ; then
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
$INTEGCB_LOCATION/.deps/bin/docker-compose --compatibility up --remove-orphans swagger-diff | tee swagger-diff.out
grep "swagger diff finished succesfully" swagger-diff.out
swaggerdiffresult=$?

echo -e "\n\033[1;96m--- Kill running Swagger Diff containers\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose --compatibility down --remove-orphans

$INTEGCB_LOCATION/.deps/bin/docker-compose --compatibility up --remove-orphans swagger-validation | tee swagger-validation-result.out
grep "swagger validation finished succesfully" swagger-validation-result.out
swaggervalidationresult=$?

echo -e "\n\033[1;96m--- Kill running Swagger Validation containers\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose --compatibility down --remove-orphans

exit $swaggerdiffresult && $swaggervalidationresult
