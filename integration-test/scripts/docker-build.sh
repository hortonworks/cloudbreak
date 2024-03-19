#!/usr/bin/env bash

: ${ADDITIONAL_DOCKER_BUILD_COMMAND:=""}

docker pull docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-21-runtime-cis:1.19-1-stig-19032024
date
echo -e "\n\033[1;96m--- build cloudbreak in docker container\033[0m\n"
docker run -i --rm \
    $ADDITIONAL_DOCKER_BUILD_COMMAND \
    -v $(pwd)/../:/tmp/prj:rw \
    -e "CM_PRIVATE_REPO_USER=$CM_PRIVATE_REPO_USER" \
    -e "CM_PRIVATE_REPO_PASSWORD=$CM_PRIVATE_REPO_PASSWORD" \
    docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-21-runtime-cis:1.19-1-stig-19032024 /tmp/prj/gradlew -b /tmp/prj/build.gradle clean build -x test
