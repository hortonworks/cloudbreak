#!/bin/bash -e
set -x
: ${WORKSPACE=.}
: ${BRANCH=rc-$(git describe --abbrev=0 --tags | cut -d '-' -f 1 | cut -d '.' -f 1,2)}

MINOR_VERSION=$(echo "$BRANCH" | cut -d'-' -f 2)
LATEST_TAG_ON_BRANCH=$(git tag -l --sort=-v:refname | grep $MINOR_VERSION | head -n 1)

if [[ $LATEST_TAG_ON_BRANCH != *"-rc"* ]]; then
    echo "released tag found: $LATEST_TAG_ON_BRANCH"
    echo "need to add '-rc.1'"
    VERSION=$LATEST_TAG_ON_BRANCH-rc.1
else
    LATEST_RC_NUMBER=$(echo $LATEST_TAG_ON_BRANCH | cut -d'.' -f 4)
    VERSION=$(echo $LATEST_TAG_ON_BRANCH | cut -d'-' -f 1)-rc.$((LATEST_RC_NUMBER+1))
fi;

git tag -a $VERSION -m "$VERSION"
git push origin $VERSION

./gradlew -Penv=jenkins -b build.gradle clean build uploadArchives -Pversion=$VERSION --info --stacktrace --parallel

echo "Computed next rc version: $VERSION"
echo VERSION=$VERSION > $WORKSPACE/version
