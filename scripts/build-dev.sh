#!/bin/bash -e
: ${WORKSPACE=.}

./gradlew -Penv=jenkins -b build.gradle clean build release uploadArchives -Prelease.scope=minor -Prelease.stage=dev --info --stacktrace --parallel

echo VERSION=$(git describe --abbrev=0 --tags) > $WORKSPACE/version
