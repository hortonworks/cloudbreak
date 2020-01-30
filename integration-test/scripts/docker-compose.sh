#!/usr/bin/env bash

set -x

: ${INTEGRATIONTEST_SUITEFILES:=${INTEGRATIONTEST_SUITE_FILES}${ADDITIONAL_SUITEFILES+,$ADDITIONAL_SUITEFILES}}
: ${INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL:=1000}
: ${INTEGCB_LOCATION?"integcb location"}

date
echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
cd $INTEGCB_LOCATION
./cbd kill
cd ..

date
echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose down

date
echo -e "\n\033[1;96m--- Create docker network\033[0m\n"
docker network create cbreak_default || true

date
echo -e "\n\033[1;96m--- Start caas mock\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up -d caas-mock

date
echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION

unset HTTPS_PROXY
env

TRACE=1 ./cbd regenerate
./cbd start-wait traefik dev-gateway core-gateway commondb vault cloudbreak environment periscope freeipa redbeams datalake

date
if [ $? -ne 0 ]; then
    echo ERROR: Failed to bring up all the necessary services! Process is about to terminate.
    ./cbd kill
    .deps/bin/docker-compose down
    exit 1
fi

cd ..

PUBLIC_IP=`echo $DOCKER_HOST | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}'`

if [[ "$PUBLIC_IP" ]]; then
    PUBLIC_IP=$PUBLIC_IP
else
    PUBLIC_IP=127.0.0.1
fi

curl -k http://${PUBLIC_IP}:8080/cb/api/swagger.json -o ./apidefinitions/cb.json
curl -k http://${PUBLIC_IP}:8088/environmentservice/api/swagger.json -o ./apidefinitions/environment.json
curl -k http://${PUBLIC_IP}:8090/freeipa/api/swagger.json -o ./apidefinitions/freeipa.json
curl -k http://${PUBLIC_IP}:8087/redbeams/api/swagger.json -o ./apidefinitions/redbeams.json
curl -k http://${PUBLIC_IP}:8086/dl/api/swagger.json -o ./apidefinitions/datalake.json
curl -k http://${PUBLIC_IP}:8085/as/api/swagger.json -o ./apidefinitions/autoscale.json

docker rm -f cbreak_periscope_1

if [[ "$CIRCLECI" ]]; then
    date
    echo -e "\n\033[1;96m--- Setting ACCESSKEY/SECRETKEY for test variables:\033[0m\n"
    export INTEGRATIONTEST_USER_ACCESSKEY="Y3JuOmFsdHVzOmlhbTp1cy13ZXN0LTE6Y2xvdWRlcmE6dXNlcjptb2NrdXNlcg=="
    export INTEGRATIONTEST_USER_SECRETKEY="nHkdxgZR0BaNHaSYM3ooS6rIlpV5E+k1CIkr+jFId2g="

    export INTEGRATIONTEST_SUITEFILES=$INTEGRATIONTEST_SUITEFILES
    export INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL=$INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL

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
        export INTEGRATIONTEST_PARALLEL=false
        export INTEGRATIONTEST_THREADCOUNT=2
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

    $INTEGCB_LOCATION/.deps/bin/docker-compose up test > test.out
    echo -e "\n\033[1;96m--- Test finished\033[0m\n"

    echo -e "\n\033[1;96m--- Collect docker stats:\033[0m\n"
    if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
        sudo chmod -R a+rwx ./test-output
        mkdir ./test-output/docker_stats
        docker stats --no-stream --format "{{ .NetIO }}" cbreak_commondb_1 > ./test-output/docker_stats/pg_stat_network_io.result;

        docker_stats_res=$(docker stats --no-stream --format "table {{ .Name }}\t{{ .Container }}\t{{ .MemUsage }}\t{{ .MemPerc }}\t{{ .CPUPerc }}\t{{ .NetIO }}\t{{ .BlockIO }}");
        docker exec cbreak_commondb_1 psql -U postgres --pset=pager=off -d cbdb -c "CREATE EXTENSION pg_stat_statements;";
        cb_pg_stat=$(docker exec cbreak_commondb_1 psql -U postgres --pset=pager=off -d cbdb -c "select * from pg_stat_statements;" --html);

        pg_stat_template=$(cat ./src/main/resources/pg_stats/pg_query_stat_template.html)
        pg_stat_result=$(echo "${pg_stat_template/DOCKER_STAT_RESULT/$docker_stats_res}")
        cbdb_stat_result=$(echo "${pg_stat_result/CB_PG_STAT/$cb_pg_stat}")
        echo "$cbdb_stat_result" > ./test-output/docker_stats/query_stat.html
    fi
fi

date
echo -e "\n\033[1;96m--- Swagger check\033[0m\n"
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-diff
$INTEGCB_LOCATION/.deps/bin/docker-compose up swagger-validation | tee swagger-validation-result.out

