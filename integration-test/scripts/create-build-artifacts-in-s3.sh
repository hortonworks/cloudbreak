#!/usr/bin/env bash
if [[ -z "$1" ]]; then
    echo "build number is unset or set to the empty string"
    exit;
fi
echo "Download artifact into build-$1.zip"
curl -Ls http://ci-cloudbreak.eng.hortonworks.com/job/cloudbreak-pull-request-builder-integration-test/$1/artifact/*zip*/archive.zip > build-$1.zip || exit
aws s3 cp build-$1.zip s3://cloudbreak-it-codebuild-artifacts
rm build-$1.zip

