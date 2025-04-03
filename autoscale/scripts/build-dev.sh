#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :autoscale:buildInfo \
  :autoscale:build \
  :autoscale:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 autoscale:test --tests=com.sequenceiq.periscope.openapi.OpenApiGenerator

aws s3 cp ./autoscale/build/openapi/autoscale.json "s3://autoscale-swagger/openapi-${VERSION}.json" --acl public-read
