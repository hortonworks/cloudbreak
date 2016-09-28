#!/bin/bash -e
: ${WORKSPACE=.}
: ${BRANCH=rc-$(git describe --abbrev=0 --tags | cut -d '-' -f 1 | cut -d '.' -f 1,2)}

MINOR_VERSION=$(echo "$BRANCH" | cut -d'-' -f 2)

if [ "$(git tag -l "$MINOR_VERSION.0-rc.1")" == '' ]; then
    ./gradlew -Penv=jenkins -b build.gradle clean build release uploadArchives -Prelease.scope=minor -Prelease.stage=rc --info --stacktrace
else
    ./gradlew -Penv=jenkins -b build.gradle clean build release uploadArchives -Prelease.scope=patch -Prelease.stage=rc --info --stacktrace
fi

echo VERSION=$(git describe --abbrev=0 --tags) > $WORKSPACE/version
