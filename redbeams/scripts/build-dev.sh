#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :redbeams:buildInfo \
  :redbeams:build \
  :redbeams:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 redbeams:test --tests=com.sequenceiq.redbeams.openapi.OpenApiGenerator

aws s3 cp ./redbeams/build/openapi/redbeams.json "s3://redbeams-swagger/openapi-${VERSION}.json" --acl public-read
