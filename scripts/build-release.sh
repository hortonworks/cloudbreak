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

./gradlew -Penv=jenkins -b build.gradle build publishBootJavaPublicationToMavenRepository :freeipa-client:publishMavenJavaPublicationToMavenRepository -Preckon.scope=$SCOPE -Preckon.stage=final --refresh-dependencies --info --stacktrace --parallel -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest
RECKONED_VERSION=$(./gradlew -Penv=jenkins -b build.gradle buildInfo -Preckon.scope=$SCOPE -Preckon.stage=final | grep Reckoned)
VERSION=${RECKONED_VERSION#Reckoned version: }

git tag -a $VERSION -m "$VERSION"
git push origin $VERSION

echo VERSION=$VERSION > $WORKSPACE/version
