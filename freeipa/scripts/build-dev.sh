#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :freeipa:buildInfo \
  :freeipa:build \
  :freeipa:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :freeipa-client:buildInfo \
  :freeipa-client:build \
  :freeipa-client:publishMavenJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 freeipa:test --tests=com.sequenceiq.freeipa.openapi.OpenApiGenerator

aws s3 cp ./freeipa/build/openapi/freeipa.json "s3://freeipa-swagger/openapi-${VERSION}.json" --acl public-read
