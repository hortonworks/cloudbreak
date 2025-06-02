#!/bin/bash -ex

export AZURE_CLIENT_ID="${AZURE_CLIENT_ID}"
export AZURE_CLIENT_SECRET="${AZURE_CLIENT_SECRET}"
export AZURE_TENANT_ID="${AZURE_TENANT_ID}"
: ${INTEGRATIONTEST_UMS_JSONSECRET_VERSION:="e556a55296e349f993d324680baa3350"}
: ${INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH:="./src/main/resources/ums-users/api-credentials.json"}
: ${INTEGRATIONTEST_UMS_JSONSECRET_NAME:="real-ums-users-dev"}
: ${IMAGE_NAME:=mcr.microsoft.com/azure-cli}
: ${GITHUB_ENV:=false}

readonly CONTAINER_NAME=fetch-ums-users

container-remove-exited() {
    declare desc="Remove Exited or Dead containers"
    local exited_containers=$(docker ps -a -f status=exited -f status=dead -q)

    if [[ -n "$exited_containers" ]]; then
        echo "Remove Exited or Dead docker containers"
        docker rm $exited_containers;
    else
        echo "There is no Exited or Dead container"
    fi
}

container-remove-stuck() {
    declare desc="Checking $CONTAINER_NAME container is running"

    if [[ "$(docker inspect -f {{.State.Running}} $CONTAINER_NAME 2> /dev/null)" == "true" ]]; then
        echo "Delete the running " $CONTAINER_NAME " container"
        docker rm -f $CONTAINER_NAME
    fi
}

init-ums-users-temp() {
    USER_JSON_PATH=${INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH#*/}
    USER_JSON_FOLDER=${USER_JSON_PATH%/*}
    echo "Updating UMS users Json file..."
    if [[ ! -d $USER_JSON_FOLDER ]]; then
        mkdir -p $USER_JSON_FOLDER
    elif [[ -f $USER_JSON_PATH ]]; then
        rm -f $USER_JSON_PATH
    fi
}

fetch-ums-secrets() {
  echo "Pull $IMAGE_NAME"
  docker pull $IMAGE_NAME

  if [[ "$GITHUB_ENV" == false ]]; then
    docker run \
        -i \
        --rm \
        --name $CONTAINER_NAME \
        -v $WORKSPACE:/prj:rw \
        -e AZURE_CLIENT_ID \
        -e AZURE_CLIENT_SECRET \
        -e AZURE_TENANT_ID \
        -e INTEGRATIONTEST_UMS_JSONSECRET_VERSION \
        -e INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH \
        -e INTEGRATIONTEST_UMS_JSONSECRET_NAME \
        -e GITHUB_ENV \
        $IMAGE_NAME /bin/bash -c "set -o pipefail ; set -ex && env && cd /prj/integration-test && eval ./scripts/fetch-ums-users.sh | tee fetchums.log"
  else
    ls -la
    docker run \
        -i \
        --rm \
        --name $CONTAINER_NAME \
        -v "$(pwd)":/prj:rw \
        -e INTEGRATIONTEST_UMS_JSONSECRET_VERSION \
        -e INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH \
        -e INTEGRATIONTEST_UMS_JSONSECRET_NAME \
        -e REAL_UMS_USERS_DEV \
        -e GITHUB_ENV \
        $IMAGE_NAME /bin/bash -c "set -o pipefail ; set -ex && env && cd prj && eval ./scripts/fetch-ums-users.sh | tee fetchums.log"
  fi
  RESULT=$?
}

main() {
  container-remove-exited
  container-remove-stuck
  init-ums-users-temp
  fetch-ums-secrets
  exit $RESULT
}

main "$@"