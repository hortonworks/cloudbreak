#!/bin/bash -e
: ${WORKSPACE=.}
: ${BRANCH=rc-$(git describe --abbrev=0 --tags | cut -d '-' -f 1 | cut -d '.' -f 1,2)}

MINOR_VERSION=$(echo "$BRANCH" | cut -d'-' -f 2)

SCOPE=
if [ "$(git tag -l "$MINOR_VERSION.0-rc.1")" == '' ]; then
    SCOPE=minor
else
    SCOPE=patch
fi

./gradlew -Penv=jenkins -b build.gradle clean build -Prelease.scope=$SCOPE -Prelease.stage=rc --info --stacktrace

# fix beanwire issue with repackaging the jar
file=$(find ./core/build/libs/ -name \*.jar)
rm -rf ${dir:=repack}
unzip -qoq $file -d $dir
jar -cfm0 ${file%.jar}-repacked.jar $dir/META-INF/MANIFEST.MF -C $dir .
mv ${file} ${file%.jar}.jar.bak
mv ${file%.jar}-repacked.jar ${file}

./gradlew -Penv=jenkins -b build.gradle release uploadArchives -Prelease.scope=$SCOPE -Prelease.stage=rc --info --stacktrace

echo VERSION=$(git describe --abbrev=0 --tags) > $WORKSPACE/version
