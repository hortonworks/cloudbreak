#!/bin/bash -x
# -e " Exit immediately if a command exits with a non-zero status.
# -x  Print commands and their arguments as they are executed.

: ${BASE_URL:=https://127.0.0.1}
: ${USERNAME_CLI:=admin@example.com}
: ${PASSWORD_CLI:=cloudbreak}
: ${CLI_TEST_FILES:=spec/integration/*.rb}

export TEST_CONTAINER_NAME=aruba-test-runner

echo "Refresh the Test Runner Docker image"
docker pull hortonworks/cloud-cli-e2e

echo "Checking stopped containers"
if [[ -n "$(docker ps -a -f status=exited -f status=dead -q)" ]]; then
  echo "Delete stopped containers"
  docker rm $(docker ps -a -f status=exited -f status=dead -q)
else
  echo "There is no Exited or Dead container"
fi

echo "Checking " $TEST_CONTAINER_NAME " container is running"
if [[ "$(docker inspect -f {{.State.Running}} $TEST_CONTAINER_NAME 2> /dev/null)" == "true" ]]; then
  echo "Delete the running " $TEST_CONTAINER_NAME " container"
  docker rm -f $TEST_CONTAINER_NAME
fi

if [[ -z "$(echo $TARGET_CBD_VERSION)" ]]; then
	export TARGET_CBD_VERSION=$(curl -sk $BASE_URL/cb/info | grep -oP "(?<=\"version\":\")[^\"]*")
	if [[ -z "$(echo $TARGET_CBD_VERSION)" ]]; then
	    export TARGET_CBD_VERSION=MOCK
	fi
fi
echo "CBD version: "$TARGET_CBD_VERSION

docker run -i \
       --rm \
       --privileged \
       --net=host \
       --name $TEST_CONTAINER_NAME \
       -v $(pwd)/aruba:/tmp \
       -v $(pwd)/../build/Linux:/usr/local/bin \
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
       hortonworks/cloud-cli-e2e
RESULT=$?

exit $RESULT