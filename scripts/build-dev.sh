#!/bin/bash -e
: ${WORKSPACE=.}

./gradlew -Penv=jenkins -b build.gradle clean build uploadArchives -Preckon.scope=minor -Preckon.stage=dev --info --stacktrace

RECKONED_VERSION=$(./gradlew -Penv=jenkins -b build.gradle buildInfo -Preckon.scope=minor -Preckon.stage=dev | grep Reckoned)
VERSION=${RECKONED_VERSION#Reckoned version: }

git tag -a $VERSION -m "$VERSION"
git push origin $VERSION

echo VERSION=$VERSION > $WORKSPACE/version
