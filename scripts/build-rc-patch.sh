#!/bin/bash -e
: ${WORKSPACE=.}
: ${BRANCH=rc-$(git describe --abbrev=0 --tags | cut -d '-' -f 1 | cut -d '.' -f 1,2)}

MINOR_VERSION=$(echo "$BRANCH" | cut -d'-' -f 2)

if [ "$(git tag -l "$MINOR_VERSION.0")" == '' ]; then
    ./gradlew -Penv=jenkins -b build.gradle clean build uploadArchives -Preckon.scope=minor -Preckon.stage=rc --info --stacktrace --parallel
    RECKONED_VERSION=$(./gradlew -Penv=jenkins -b build.gradle buildInfo -Preckon.scope=minor -Preckon.stage=rc | grep Reckoned)
else
    ./gradlew -Penv=jenkins -b build.gradle clean build uploadArchives -Preckon.scope=patch -Preckon.stage=rc --info --stacktrace --parallel
    RECKONED_VERSION=$(./gradlew -Penv=jenkins -b build.gradle buildInfo -Preckon.scope=patch -Preckon.stage=rc | grep Reckoned)
fi

VERSION=${RECKONED_VERSION#Reckoned version: }

git tag -a $VERSION -m "$VERSION"
git push origin $VERSION

echo VERSION=$VERSION > $WORKSPACE/version
