#!/usr/bin/env bash

set -x

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

echo -e "\n\033[1;96m--- Swagger check\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-diff
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-validation | tee swagger-validation-result.out

