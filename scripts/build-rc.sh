#!/bin/bash
: ${WORKSPACE=.}


VERSION=$(git describe --abbrev=0 --tags | cut -d '-' -f 1 | cut -d '.' -f 1,2)
git checkout -b "rc-$VERSION"
git push -u origin "rc-$VERSION"

./gradlew -Penv=jenkins -b build.gradle clean build release uploadArchives -Prelease.scope=minor -Prelease.stage=rc --info --stacktrace

echo VERSION=$(git describe --abbrev=0 --tags) > $WORKSPACE/version