#!/usr/bin/env bash

export DEBUG=1

set -ex

: ${INTEGRATIONTEST_SUITEFILES:=${INTEGRATIONTEST_SUITE_FILES}${ADDITIONAL_SUITEFILES+,$ADDITIONAL_SUITEFILES}}
: ${INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL:=1000}
: ${INTEGCB_LOCATION?"integcb location"}
: ${INTEGRATIONTEST_USER_ACCESSKEY:="Y3JuOmFsdHVzOmlhbTp1cy13ZXN0LTE6Y2xvdWRlcmE6dXNlcjptb2NrdXNlckB1bXMubW9jaw=="}
: ${INTEGRATIONTEST_USER_SECRETKEY:="nHkdxgZR0BaNHaSYM3ooS6rIlpV5E+k1CIkr+jFId2g="}

date
echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
cd $INTEGCB_LOCATION
./cbd kill
cd ..

date
echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose --compatibility down --remove-orphans

date
echo -e "\n\033[1;96m--- Create docker network\033[0m\n"
docker network create cbreak_default || true

date
echo -e "\n\033[1;96m--- Start thunderhead mock\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose --compatibility up --remove-orphans --detach thunderhead-mock

date
echo -e "\n\033[1;96m--- Copy mock infrastructure infrastructure-mock.p12 cert to certs dir\033[0m\n"
mkdir -p $INTEGCB_LOCATION/certs/trusted
cp ../mock-infrastructure/src/main/resources/keystore/infrastructure-mock.cer $INTEGCB_LOCATION/certs/trusted/infrastructure-mock.cer

date
echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION

unset HTTPS_PROXY
env

TRACE=1 ./cbd regenerate
./cbd start-wait traefik dev-gateway core-gateway commondb vault cloudbreak environment periscope freeipa redbeams datalake haveged mock-infrastructure

docker ps --format ‘{{.Image}}’

date
if [ $? -ne 0 ]; then
    echo -e "\n\033[1;96m--- ERROR: Failed to bring up all the necessary services! Process is about to terminate!\033[0m\n"
    ./cbd kill
    .deps/bin/docker-compose --compatibility down --remove-orphans
    exit 1
fi

cd ..

if [[ "$DOCKER_HOST" ]]; then
    PUBLIC_IP=`echo $DOCKER_HOST | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}'`
fi
if [[ "$PUBLIC_IP" ]]; then
    PUBLIC_IP=$PUBLIC_IP
else
    PUBLIC_IP=127.0.0.1
fi

curl -k http://${PUBLIC_IP}:8080/cb/api/swagger.json -o ./apidefinitions/cloudbreak.json
curl -k http://${PUBLIC_IP}:8088/environmentservice/api/swagger.json -o ./apidefinitions/environment.json
curl -k http://${PUBLIC_IP}:8090/freeipa/api/swagger.json -o ./apidefinitions/freeipa.json
curl -k http://${PUBLIC_IP}:8087/redbeams/api/swagger.json -o ./apidefinitions/redbeams.json
curl -k http://${PUBLIC_IP}:8086/dl/api/swagger.json -o ./apidefinitions/datalake.json
curl -k http://${PUBLIC_IP}:8085/as/api/swagger.json -o ./apidefinitions/autoscale.json

docker rm -f cbreak_periscope_1

if [[ "$CIRCLECI" ]]; then
    date
    echo -e "\n\033[1;96m--- Setting ACCESSKEY/SECRETKEY for test variables:\033[0m\n"
    export INTEGRATIONTEST_USER_ACCESSKEY=$INTEGRATIONTEST_USER_ACCESSKEY
    export INTEGRATIONTEST_USER_SECRETKEY=$INTEGRATIONTEST_USER_SECRETKEY

    export INTEGRATIONTEST_SUITEFILES=$INTEGRATIONTEST_SUITEFILES
    export INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL=$INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL

    export INTEGRATIONTEST_UMS_ACCOUNTKEY=$INTEGRATIONTEST_UMS_ACCOUNTKEY
    export INTEGRATIONTEST_UMS_DEPLOYMENTKEY=$INTEGRATIONTEST_UMS_DEPLOYMENTKEY
    export INTEGRATIONTEST_UMS_JSONSECRET_VERSION=$INTEGRATIONTEST_UMS_JSONSECRET_VERSION
    export INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH=$INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH
    export INTEGRATIONTEST_UMS_JSONSECRET_NAME=$INTEGRATIONTEST_UMS_JSONSECRET_NAME

    if [[ -n "${INTEGRATIONTEST_YARN_QUEUE}" ]]; then
        date
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
    elif [[ "$AWS" == true ]]; then
        export INTEGRATIONTEST_PARALLEL=true
        export INTEGRATIONTEST_THREADCOUNT=4
        export INTEGRATIONTEST_CLOUDPROVIDER="AWS"
    else
        export INTEGRATIONTEST_PARALLEL=methods
        export INTEGRATIONTEST_CLOUDPROVIDER="MOCK"
    fi

    date
    echo -e "\n\033[1;96m--- Env variables started with INTEGRATIONTEST :\033[0m\n"
    env | grep -i INTEGRATIONTEST

    date
    echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
    echo $INTEGRATIONTEST_SUITEFILES

    date
    echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
    rm -rf test-output

    export DOCKER_CLIENT_TIMEOUT=120
    export COMPOSE_HTTP_TIMEOUT=120

    set -o pipefail ; $INTEGCB_LOCATION/.deps/bin/docker-compose --compatibility up --remove-orphans --exit-code-from test test | tee test.out
    echo -e "\n\033[1;96m--- Test finished\033[0m\n"

    echo -e "\n\033[1;96m--- Collect docker stats:\033[0m\n"
    if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
        sudo mkdir -p ./test-output
        sudo chmod -R a+rwx ./test-output
        mkdir ./test-output/docker_stats
        docker stats --no-stream --format "{{ .NetIO }}" cbreak_commondb_1 > ./test-output/docker_stats/pg_stat_network_io.result;

        docker stats --no-stream --format "table {{ .Name }}\t{{ .Container }}\t{{ .MemUsage }}\t{{ .MemPerc }}\t{{ .CPUPerc }}\t{{ .NetIO }}\t{{ .BlockIO }}" > ./test-output/docker_stats/docker_stat.html
        docker exec cbreak_commondb_1 psql -U postgres --pset=pager=off -d cbdb -c "CREATE EXTENSION pg_stat_statements;";
        docker exec cbreak_commondb_1 psql -U postgres --pset=pager=off -d cbdb -c "select * from pg_stat_statements;" --html > ./test-output/docker_stats/query_stat.html

        cp ./src/main/resources/pg_stats/pg_query_stat_template.html ./test-output/docker_stats/pg_query_stat_template.html

        #FIXME, might be better not to use in place sed
        sed -i '/<!-- CB_PG_STAT -->/r ./test-output/docker_stats/query_stat.html' ./test-output/docker_stats/pg_query_stat_template.html
        sed -i '/<!-- DOCKER_STAT_RESULT -->/r ./test-output/docker_stats/docker_stat.html' ./test-output/docker_stats/pg_query_stat_template.html

        mv ./test-output/docker_stats/pg_query_stat_template.html ./test-output/docker_stats/query_stat.html
    fi
fi

