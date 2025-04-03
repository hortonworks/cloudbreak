#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :environment:buildInfo \
  :environment:build \
  :environment:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 environment:test --tests=com.sequenceiq.environment.openapi.OpenApiGenerator

aws s3 cp ./environment/build/openapi/environment.json "s3://environment-swagger/openapi-${VERSION}.json" --acl public-read
