#!/bin/bash -e
set -x

./gradlew -Penv=jenkins -b build.gradle buildInfo build uploadBootArchives -Pversion=$VERSION --parallel --stacktrace -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest

if [[ "${RUN_SONARQUBE}" == "true" ]]; then
    ./gradlew -Penv=jenkins -b build.gradle core:sonarqube core:jacocoTestReport
    ./gradlew -Penv=jenkins -b build.gradle freeipa:sonarqube freeipa:jacocoTestReport
    ./gradlew -Penv=jenkins -b build.gradle autoscale:sonarqube autoscale:jacocoTestReport
    ./gradlew -Penv=jenkins -b build.gradle datalake:sonarqube datalake:jacocoTestReport
    ./gradlew -Penv=jenkins -b build.gradle environment:sonarqube environment:jacocoTestReport
    ./gradlew -Penv=jenkins -b build.gradle redbeams:sonarqube redbeams:jacocoTestReport
fi

aws s3 cp ./core/build/swagger/cb.json "s3://cloudbreak-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./environment/build/swagger/environment.json "s3://environment-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./freeipa/build/swagger/freeipa.json "s3://freeipa-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./redbeams/build/swagger/redbeams.json "s3://redbeams-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./datalake/build/swagger/datalake.json "s3://datalake-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./autoscale/build/swagger/autoscale.json "s3://autoscale-swagger/swagger-${VERSION}.json" --acl public-read
