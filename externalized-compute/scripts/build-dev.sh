#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :externalized-compute:buildInfo \
  :externalized-compute:build \
  :externalized-compute:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 externalized-compute:test --tests=com.sequenceiq.externalizedcompute.openapi.OpenApiGenerator

aws s3 cp ./externalized-compute/build/openapi/externalizedcompute.json "s3://externalizedcompute-swagger/openapi-${VERSION}.json" --acl public-read