#!/bin/bash
: ${WORKSPACE=.}

git checkout "rc-$VERSION" || exit 1
git merge --ff-only "origin/rc-$VERSION"

# if release-$VERSION branch not exists
if ! git rev-parse --verify "origin/release-$VERSION" &> /dev/null; then
  echo "branch will be created"
  git checkout -b "release-$VERSION"
  git push -u origin "release-$VERSION"
  SCOPE=minor
else
  echo "branch exists"
  git checkout "release-$VERSION"
  git merge --ff-only "origin/rc-$VERSION"
  git push -u origin "release-$VERSION"
  SCOPE=patch
fi

./gradlew -Penv=jenkins -b build.gradle clean build release uploadArchives -Prelease.scope=$SCOPE -Prelease.stage=final --refresh-dependencies --info --stacktrace

echo VERSION=$(git describe --abbrev=0 --tags) > $WORKSPACE/version
