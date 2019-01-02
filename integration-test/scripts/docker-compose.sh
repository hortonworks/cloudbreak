#!/usr/bin/env bash

set -x

: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
cd $INTEGCB_LOCATION
./cbd kill
cd ..

echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose down

echo -e "\n\033[1;96m--- Start caas mock\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up -d mock-caas

echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION
./cbd regenerate
./cbd start-wait consul registrator identity commondb vault cloudbreak
cd ..

echo -e "\n\033[1;96m--- Get token for testing from caas mock\033[0m\n"
publicip=$(cat $INTEGCB_LOCATION/Profile | grep PUBLIC_IP= | cut -d "=" -f 2)
export INTEGRATIONTEST_CAAS_TOKEN=$(curl http://$publicip:10080/oidc/authorize?username=mock@hortonworks.com\&tenant=hortonworks)
export INTEGRATIONTEST_CAAS_PROTOCOL=http

echo -e "\n\033[1;96m--- Caas variables:\033[0m\n"
echo INTEGRATIONTEST_CAAS_TOKEN=$INTEGRATIONTEST_CAAS_TOKEN
echo INTEGRATIONTEST_CAAS_PROTOCOL=$INTEGRATIONTEST_CAAS_PROTOCOL

export INTEGRATIONTEST_SUITEFILES=file:/it/src/main/resources/testsuites/v2/mock/all-in-mock-package.yaml${ADDITIONAL_SUITEFILES+,$ADDITIONAL_SUITEFILES}
export INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL=1000

echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
echo $INTEGRATIONTEST_SUITEFILES

echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
rm -rf test-output

$INTEGCB_LOCATION/.deps/bin/docker-compose up test > test.out
echo -e "\n\033[1;96m--- Test finished\033[0m\n"

echo -e "\n\033[1;96m--- Swagger check\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-diff
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-validation | tee swagger-validation-result.out

