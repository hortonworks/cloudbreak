#!/usr/bin/env bash

set -x

: ${INTEGCB_LOCATION?"integcb location"}

cd $INTEGCB_LOCATION
echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
./cbd kill

echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
cd ..
$INTEGCB_LOCATION/.deps/bin/docker-compose down

export INTEGRATIONTEST_SUITEFILES=file:/it/src/main/resources/testsuites/blueprinttests.yaml,file:/it/src/main/resources/testsuites/recipetests.yaml
export INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL=1000

echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
echo $INTEGRATIONTEST_SUITEFILES

echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION
./cbd regenerate
./cbd start-wait consul registrator identity commondb cloudbreak

echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
cd ..
rm -rf test-output
$INTEGCB_LOCATION/.deps/bin/docker-compose up test > test.out
echo -e "\n\033[1;96m--- Test finished\033[0m\n"

echo -e "\n\033[1;96m--- Swagger check\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-diff
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-validation | tee swagger-validation-result.out

