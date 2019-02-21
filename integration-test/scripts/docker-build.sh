#!/usr/bin/env bash

: ${ADDITIONAL_DOCKER_BUILD_COMMAND:=""}

docker pull openjdk:11-jdk
echo -e "\n\033[1;96m--- build cloudbreak in docker container\033[0m\n"
docker run -i --rm $ADDITIONAL_DOCKER_BUILD_COMMAND -v $(pwd)/../:/tmp/prj:rw openjdk:11-jdk /tmp/prj/gradlew -b /tmp/prj/build.gradle clean build -x test
