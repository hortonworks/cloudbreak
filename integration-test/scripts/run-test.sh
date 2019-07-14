#!/usr/bin/env bash

java -jar /it/build/libs/cloudbreak-integration-test.jar \
    com.sequenceiq.it.IntegrationTestApp \
    --integrationtest.command=suiteurls \
    --integrationtest.suiteFiles=${INTEGRATIONTEST_SUITEFILES} \
    >> /test.out 2>&1