#!/usr/bin/env bash

curl -o wait-for-it.sh https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh
chmod +x wait-for-it.sh
./wait-for-it.sh -t 300 $CLOUDBREAK_URL -- echo "cloudbreak is up"

java -jar /it/build/libs/cloudbreak-integration-test.jar com.sequenceiq.it.IntegrationTestApp --integrationtest.outputdir=/it --integrationtest.defaultBlueprintName=hdp-small-default --integrationtest.command=suites --integrationtest.suiteFiles=/it/src/main/resources/testsuites/mock/mock-clustercreate.yaml
