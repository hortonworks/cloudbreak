#!/bin/bash -e
set -x

echo "Build version: $VERSION"

./gradlew -Penv=jenkins -b build.gradle buildInfo build uploadArchives -Pversion=$VERSION --info --stacktrace --parallel -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest

aws s3 cp ./core/build/swagger/cb.json "s3://cloudbreak-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./environment/build/swagger/environment.json "s3://environment-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./datalake/build/swagger/freeipa.json "s3://freeipa-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./freeipa/build/swagger/redbeams.json "s3://redbeams-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./redbeams/build/swagger/datalake.json "s3://datalake-swagger/swagger-${VERSION}.json" --acl public-read
aws s3 cp ./autoscale/build/swagger/autoscale.json "s3://autoscale-swagger/swagger-${VERSION}.json" --acl public-read
