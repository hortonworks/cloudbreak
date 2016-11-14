#!/usr/bin/env bash

: ${ADDITIONAL_DOCKER_BUILD_COMMAND:=""}

echo -e "\n\033[1;96m--- build cloudbreak in docker container\033[0m\n"
docker run -i --rm $ADDITIONAL_DOCKER_BUILD_COMMAND -v $(pwd)/../:/tmp/prj:rw java:openjdk-8 /tmp/prj/gradlew -b /tmp/prj/build.gradle clean build -x test