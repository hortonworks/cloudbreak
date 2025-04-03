#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :core:buildInfo \
  :core:build \
  :core:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 core:test --tests=com.sequenceiq.cloudbreak.openapi.OpenApiGenerator

aws s3 cp ./core/build/openapi/cb.json "s3://cloudbreak-swagger/openapi-${VERSION}.json" --acl public-read
