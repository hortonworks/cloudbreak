#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -Phttps.socketTimeout=720000 -Phttps.connectionTimeout=720000 -b build.gradle buildInfo build \
  publishBootJavaPublicationToMavenRepository \
  :freeipa-client:publishMavenJavaPublicationToMavenRepository \
  -Pversion=$VERSION --parallel --stacktrace \
  core:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  environment:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  freeipa:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  redbeams:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  datalake:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  autoscale:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  externalized-compute:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  environment-remote:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
  -x test \
  -x checkstyleMain \
  -x checkstyleTest \
  -x spotbugsMain \
  -x spotbugsTest

if [[ "${RUN_SONARQUBE}" == "true" ]]; then
    # removing core modul because that is instable
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle core:sonarqube core:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle freeipa:sonarqube freeipa:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle autoscale:sonarqube autoscale:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle datalake:sonarqube datalake:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle environment:sonarqube environment:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle redbeams:sonarqube redbeams:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle externalized-compute:sonarqube externalized-compute:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
    ./gradlew -Penv=jenkins -Phttp.socketTimeout=300000 -Phttp.connectionTimeout=300000 -b build.gradle environment-remote:sonarqube environment-remote:jacocoTestReport -Dorg.gradle.internal.http.socketTimeout=600000 -Dorg.gradle.internal.http.connectionTimeout=600000 -x test || true
fi

aws s3 cp ./core/build/openapi/cb.json "s3://cloudbreak-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./environment/build/openapi/environment.json "s3://environment-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./freeipa/build/openapi/freeipa.json "s3://freeipa-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./redbeams/build/openapi/redbeams.json "s3://redbeams-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./datalake/build/openapi/datalake.json "s3://datalake-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./autoscale/build/openapi/autoscale.json "s3://autoscale-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./externalized-compute/build/openapi/externalizedcompute.json "s3://externalizedcompute-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./environment-remote/build/openapi/remoteenvironment.json "s3://remoteenvironment-swagger/openapi-${VERSION}.json" --acl public-read
