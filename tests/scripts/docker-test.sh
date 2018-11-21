#!/bin/bash -x
# -e " Exit immediately if a command exits with a non-zero status.
# -x  Print commands and their arguments as they are executed.

: ${BASE_URL:=https://127.0.0.1}
: ${USERNAME_CLI:=admin@example.com}
: ${PASSWORD_CLI:=cloudbreak}
: ${DOCKER_TAG:=latest}
: ${CLI_TEST_FILES:=spec/integration/*.rb}
: ${BLUEPRINT_URL:? required}
: ${RECIPE_URL:? required}

readonly TEST_CONTAINER_NAME=cli-test-runner

image-tag() {
    declare desc="Set CLI Docker Image Tag based for CBD version [for Jenkins E2E]"

    if [[ $TARGET_CBD_VERSION ]]; then
	    export DOCKER_TAG=$TARGET_CBD_VERSION
    fi
    if [[ $BRANCH || $(git rev-parse --abbrev-ref HEAD) == *"master"* ]]; then
	    export DOCKER_TAG=latest
    fi
}

image-update() {
    declare desc="Refresh the Test Runner Docker image"

    docker pull hortonworks/cloud-cli-e2e:$DOCKER_TAG
}

image-cleanup() {
    declare desc="Removes all exited containers and old images"

    container-remove-stuck
    container-remove-exited

    docker rmi $(docker images -q -f dangling=true)
    docker images | grep cloud-cli-e2e | tr -s ' ' | cut -d ' ' -f 2 | xargs -I {} docker rmi hortonworks/cloud-cli-e2e:{}
}

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
    declare desc="Checking $TEST_CONTAINER_NAME container is running"

    if [[ "$(docker inspect -f {{.State.Running}} $TEST_CONTAINER_NAME 2> /dev/null)" == "true" ]]; then
        echo "Delete the running " $TEST_CONTAINER_NAME " container"
        docker rm -f $TEST_CONTAINER_NAME
    fi
}

cbd-version() {
    if [[ -z "$(echo $TARGET_CBD_VERSION)" ]]; then
	    export TARGET_CBD_VERSION=$(curl -sk $BASE_URL/cb/info | grep -oP "(?<=\"version\":\")[^\"]*")

	    if [[ -z "$(echo $TARGET_CBD_VERSION)" ]]; then
	        export TARGET_CBD_VERSION=MOCK
	    fi
    fi
    echo "CBD version: "$TARGET_CBD_VERSION
}

test-regression() {
    docker run -i \
       --rm \
       --privileged \
       --net=host \
       --name $TEST_CONTAINER_NAME \
       -v $(pwd):/aruba \
       -v $(pwd)/scripts/aruba-docker.sh:/entrypoint.sh \
       -v $(pwd)/responses:/responses \
       -v $(pwd)/requests:/requests \
       -v $(pwd)/../build/Linux:/usr/local/bin \
       -v /var/run/docker.sock:/var/run/docker.sock \
       -e "BASE_URL=$BASE_URL" \
       -e "USERNAME_CLI=$USERNAME_CLI" \
       -e "PASSWORD_CLI=$PASSWORD_CLI" \
       -e "OS_V2_ENDPOINT=$OS_V2_ENDPOINT" \
       -e "OS_V2_USERNAME=$OS_V2_USERNAME" \
       -e "OS_V2_PASSWORD=$OS_V2_PASSWORD" \
       -e "OS_V2_TENANT_NAME=$OS_V2_TENANT_NAME" \
       -e "OS_V3_ENDPOINT=$OS_V3_ENDPOINT" \
       -e "OS_V3_USERNAME=$OS_V3_USERNAME" \
       -e "OS_V3_PASSWORD=$OS_V3_PASSWORD" \
       -e "OS_V3_KEYSTONE_SCOPE=$OS_V3_KEYSTONE_SCOPE" \
       -e "OS_V3_USER_DOMAIN=$OS_V3_USER_DOMAIN" \
       -e "OS_V3_PROJECT_NAME=$OS_V3_PROJECT_NAME" \
       -e "OS_V3_PROJECT_DOMAIN=$OS_V3_PROJECT_DOMAIN" \
       -e "OS_APIFACING=$OS_APIFACING" \
       -e "OS_REGION=$OS_REGION" \
       -e "AWS_ROLE_ARN=$AWS_ROLE_ARN" \
       -e "TARGET_CBD_VERSION=$TARGET_CBD_VERSION" \
       -e "INTEGRATIONTEST_RDSCONFIG_RDSUSER=$INTEGRATIONTEST_RDSCONFIG_RDSUSER" \
       -e "INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD=$INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD" \
       -e "INTEGRATIONTEST_RDSCONFIG_RDSCONNECTIONURL=$INTEGRATIONTEST_RDSCONFIG_RDSCONNECTIONURL" \
       -e "INTEGRATIONTEST_LDAPCONFIG_LDAPSERVERHOST=$INTEGRATIONTEST_LDAPCONFIG_LDAPSERVERHOST" \
       -e "INTEGRATIONTEST_LDAPCONFIG_BINDPASSWORD=$INTEGRATIONTEST_LDAPCONFIG_BINDPASSWORD" \
       -e "INTEGRATIONTEST_PROXYCONFIG_PROXYHOST=$INTEGRATIONTEST_PROXYCONFIG_PROXYHOST" \
       -e "INTEGRATIONTEST_PROXYCONFIG_PROXYUSER=$INTEGRATIONTEST_PROXYCONFIG_PROXYUSER" \
       -e "INTEGRATIONTEST_PROXYCONFIG_PROXYPASSWORD=$INTEGRATIONTEST_PROXYCONFIG_PROXYPASSWORD" \
       -e "CLI_TEST_FILES=$CLI_TEST_FILES" \
       -e "BLUEPRINT_URL=$BLUEPRINT_URL" \
       -e "RECIPE_URL=$RECIPE_URL" \
       hortonworks/cloud-cli-e2e:$DOCKER_TAG
    RESULT=$?
}

main() {
    image-tag
    image-cleanup
    image-update
    cbd-version
    test-regression
    exit $RESULT
}

main "$@"
