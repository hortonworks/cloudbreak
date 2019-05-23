#!/usr/bin/env bash

set -x

: ${INTEGRATIONTEST_SUITEFILES:=file:/it/src/main/resources/testsuites/v4/mock/all-in-mock-package.yaml${ADDITIONAL_SUITEFILES+,$ADDITIONAL_SUITEFILES}}
: ${INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL:=1000}
: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
cd $INTEGCB_LOCATION
./cbd kill
cd ..

echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose down

echo -e "\n\033[1;96m--- Create docker network\033[0m\n"
docker network create cbreak_default || true

echo -e "\n\033[1;96m--- Start caas mock\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up -d caas-mock

echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION
./cbd regenerate
./cbd start-wait identity commondb vault cloudbreak
cd ..

if [[ "$CIRCLECI" ]]; then
    echo -e "\n\033[1;96m--- Setting User CRN for test variables:\033[0m\n"
    export INTEGRATIONTEST_USER_CRN="crn:altus:iam:us-west-1:default:user:mockuser@cloudera.com"

    export INTEGRATIONTEST_SUITEFILES=$INTEGRATIONTEST_SUITEFILES
    export INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL=$INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL


    if [[ -n "${INTEGRATIONTEST_YARN_QUEUE}" ]]; then
        echo -e "\n\033[1;96m--- YARN smoke testing variables:\033[0m\n"
        export CM_PRIVATE_REPO_USER=$CM_PRIVATE_REPO_USER
        export CM_PRIVATE_REPO_PASSWORD=$CM_PRIVATE_REPO_PASSWORD
        export INTEGRATIONTEST_CLOUDPROVIDER=$INTEGRATIONTEST_CLOUDPROVIDER
        export INTEGRATIONTEST_YARN_DEFAULTBLUEPRINTNAME=$INTEGRATIONTEST_YARN_DEFAULTBLUEPRINTNAME
        export INTEGRATIONTEST_YARN_QUEUE=$INTEGRATIONTEST_YARN_QUEUE
        export INTEGRATIONTEST_YARN_IMAGECATALOGURL=$INTEGRATIONTEST_YARN_IMAGECATALOGURL
        export INTEGRATIONTEST_YARN_IMAGEID=$INTEGRATIONTEST_YARN_IMAGEID
        export INTEGRATIONTEST_YARN_REGION=$INTEGRATIONTEST_YARN_REGION
        export INTEGRATIONTEST_YARN_LOCATION=$INTEGRATIONTEST_YARN_LOCATION
    else
        export INTEGRATIONTEST_CLOUDPROVIDER="MOCK"
    fi

    echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
    echo $INTEGRATIONTEST_SUITEFILES

    echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
    rm -rf test-output

    $INTEGCB_LOCATION/.deps/bin/docker-compose up test > test.out
    echo -e "\n\033[1;96m--- Test finished\033[0m\n"
fi

echo -e "\n\033[1;96m--- Swagger check\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-diff
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-validation | tee swagger-validation-result.out

