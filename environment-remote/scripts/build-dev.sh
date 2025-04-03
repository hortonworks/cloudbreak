#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle \
  :environment-remote:buildInfo \
  :environment-remote:build \
  :environment-remote:publishBootJavaPublicationToMavenRepository \
  -Pversion=$VERSION \
  --parallel \
  --stacktrace \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

./gradlew -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 environment-remote:test --tests=com.sequenceiq.remoteenvironment.openapi.OpenApiGenerator

aws s3 cp ./environment-remote/build/openapi/remoteenvironment.json "s3://remoteenvironment-swagger/openapi-${VERSION}.json" --acl public-read
