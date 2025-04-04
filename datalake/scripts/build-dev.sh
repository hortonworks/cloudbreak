#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :datalake:buildInfo \
  :datalake:build \
  :datalake:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 datalake:test --tests=com.sequenceiq.datalake.openapi.OpenApiGenerator

aws s3 cp ./datalake/build/openapi/datalake.json "s3://datalake-swagger/openapi-${VERSION}.json" --acl public-read
