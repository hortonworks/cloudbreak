#!/bin/bash -e
set -x
: ${WORKSPACE=.}
: ${BRANCH=rc-$(git describe --abbrev=0 --tags | cut -d '-' -f 1 | cut -d '.' -f 1,2)}

MINOR_VERSION=$(echo "$BRANCH" | cut -d'-' -f 2)
LATEST_RELEASED_TAG_ON_BRANCH=$(git tag -l --sort=-v:refname | grep ^$MINOR_VERSION | awk '!/rc/ && !/dev/' | head -n 1)
LATEST_RC_TAG_ON_BRANCH=$(git tag -l --sort=-v:refname | grep ^$MINOR_VERSION | grep 'rc' | head -n 1)

echo "$LATEST_RC_TAG_ON_BRANCH"
if [[ -z $LATEST_RC_TAG_ON_BRANCH ]]; then
    echo "no '-rc' tag found on branch $BRANCH"
    echo "need to add '-rc.1'"
    VERSION=$MINOR_VERSION.0-rc.1
else
    if [[ $LATEST_RELEASED_TAG_ON_BRANCH = $(echo $LATEST_RC_TAG_ON_BRANCH | cut -d'-' -f 1) ]]; then
        echo "need to increase version with reckon from $LATEST_RELEASED_TAG_ON_BRANCH"
        RECKONED_VERSION=$(./gradlew -Penv=jenkins -b build.gradle buildInfo -Preckon.scope=patch -Preckon.stage=rc | grep Reckoned)
        VERSION=${RECKONED_VERSION#Reckoned version: }
    else
        LATEST_RC_NUMBER=$(echo $LATEST_RC_TAG_ON_BRANCH | cut -d'.' -f 4)
        VERSION=$(echo $LATEST_RC_TAG_ON_BRANCH | cut -d'-' -f 1)-rc.$((LATEST_RC_NUMBER+1))
    fi;
fi;

git tag -a $VERSION -m "$VERSION"
git push origin $VERSION

echo "Computed next rc version: $VERSION"
echo VERSION=$VERSION > $WORKSPACE/version
