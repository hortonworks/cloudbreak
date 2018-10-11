#!/usr/bin/env bash

set -x

: ${INTEGCB_LOCATION?"integcb location"}

cd $INTEGCB_LOCATION
echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
./cbd kill

echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
cd ..
$INTEGCB_LOCATION/.deps/bin/docker-compose down

export INTEGRATIONTEST_SUITEFILES=file:/it/src/main/resources/testsuites/v2/mock/v2-mock-stackcreate-scaling.yaml,file:/it/src/main/resources/testsuites/v2/mock/v2-mock-knoxgateway-stackcreate.yaml,file:/it/src/main/resources/testsuites/v2/mock/v2-mock-kerberized-stackcreate-scaling.yaml,file:/it/src/main/resources/testsuites/blueprinttests.yaml,file:/it/src/main/resources/testsuites/recipetests.yaml,file:/it/src/main/resources/testsuites/repoconfigstests.yaml,file:/it/src/main/resources/testsuites/disktypetests.yaml,file:/it/src/main/resources/testsuites/securityruletests.yaml,file:/it/src/main/resources/testsuites/v2/mock/clustermocktestset.yaml,file:/it/src/main/resources/testsuites/v2/mock/v2-mock-stack-maintenance-mode.yaml${ADDITIONAL_SUITEFILES+,$ADDITIONAL_SUITEFILES}

echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
echo $INTEGRATIONTEST_SUITEFILES

echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION
sudo ./cbd regenerate
sudo ./cbd start-wait consul registrator identity commondb cloudbreak

echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
cd ..
rm -rf test-output
$INTEGCB_LOCATION/.deps/bin/docker-compose up test > test.out
echo -e "\n\033[1;96m--- Test finished\033[0m\n"

echo -e "\n\033[1;96m--- Swagger check\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-diff
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-validation | tee swagger-validation-result.out

