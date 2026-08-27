#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :maintenance:buildInfo \
  :maintenance:build \
  :maintenance:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 maintenance:test --tests=com.sequenceiq.maintenance.openapi.OpenApiGenerator

aws s3 cp ./maintenance/build/openapi/maintenance.json "s3://maintenance-swagger/openapi-${VERSION}.json" --acl public-read
