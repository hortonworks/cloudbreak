#!/bin/bash -e
: ${WORKSPACE=.}

set -x

LATEST_RC_BRANCH=$(git branch --sort=-refname -r | grep 'origin/rc' | head -n 1)
LATEST_RC_MAJOR_MINOR_VERSION=$(echo "$LATEST_RC_BRANCH" | cut -d'-' -f 2)
LATEST_RC_MAJOR=$(echo $LATEST_RC_MAJOR_MINOR_VERSION | cut -d'.' -f 1)
LATEST_RC_MINOR=$(echo $LATEST_RC_MAJOR_MINOR_VERSION | cut -d'.' -f 2)
INCREASED_MINOR=$((LATEST_RC_MINOR+1))
ACTUAL_VERSION="$LATEST_RC_MAJOR.$INCREASED_MINOR"

if [ "$(git tag -l $ACTUAL_VERSION.0-dev.1)" == '' ]; then
    echo "no dev version found: $ACTUAL_VERSION"
    VERSION=$ACTUAL_VERSION.0-dev.1-pipeline-poc
else
    LATEST_DEV_VERSION=$(echo $(git tag -l --sort=-v:refname | grep "$ACTUAL_VERSION\\.0-dev" | head -n 1))
    LATEST_DEV_NUMBER=$(echo $LATEST_DEV_VERSION | cut -d'.' -f 4)
    VERSION=$ACTUAL_VERSION.0-dev.$((LATEST_DEV_NUMBER+1))-pipeline-poc
fi;

git tag -a $VERSION -m "$VERSION"
git push origin $VERSION

./gradlew -Penv=jenkins -b build.gradle clean build uploadArchives -Pversion=$VERSION --info --stacktrace --parallel

echo "Computed next dev version (for pipeline testing purposes): $VERSION"
echo VERSION=$VERSION > $WORKSPACE/version
