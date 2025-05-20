#!/usr/bin/env bash

: ${ADDITIONAL_DOCKER_BUILD_COMMAND:=""}

docker pull docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-runtime-nofips
date
echo -e "\n\033[1;96m--- build cloudbreak in docker container\033[0m\n"
docker run -i --rm \
    $ADDITIONAL_DOCKER_BUILD_COMMAND \
    -v $(pwd)/../:/tmp/prj:rw \
    -e "CM_PRIVATE_REPO_USER=$CM_PRIVATE_REPO_USER" \
    -e "CM_PRIVATE_REPO_PASSWORD=$CM_PRIVATE_REPO_PASSWORD" \
    docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-runtime-nofips /tmp/prj/gradlew \
    -b /tmp/prj/build.gradle clean build \
    --quiet \
    --no-daemon \
    --parallel \
    --warning-mode none \
    -x test \
    -x checkstyleMain \
    -x checkstyleTest \
    -x spotbugsMain \
    -x spotbugsTest
