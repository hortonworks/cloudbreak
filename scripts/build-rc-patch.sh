#!/bin/bash -e
set -x

echo "Build version: $VERSION"

./gradlew -Penv=jenkins -b build.gradle buildInfo build publishBootJavaPublicationToMavenRepository -Pversion=$VERSION --info --stacktrace --parallel -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest

aws s3 cp ./core/build/openapi/cb.json "s3://cloudbreak-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./environment/build/openapi/environment.json "s3://environment-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./freeipa/build/openapi/freeipa.json "s3://freeipa-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./redbeams/build/openapi/redbeams.json "s3://redbeams-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./datalake/build/openapi/datalake.json "s3://datalake-swagger/openapi-${VERSION}.json" --acl public-read
aws s3 cp ./autoscale/build/openapi/autoscale.json "s3://autoscale-swagger/openapi-${VERSION}.json" --acl public-read
