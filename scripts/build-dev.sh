#!/bin/bash -e
: ${WORKSPACE=.}

./gradlew -Penv=jenkins -b build.gradle clean build release uploadArchives -Prelease.scope=major -Prelease.stage=dev --info --stacktrace

echo VERSION=$(git describe --abbrev=0 --tags) > $WORKSPACE/version
