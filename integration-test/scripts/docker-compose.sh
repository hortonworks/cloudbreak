#!/usr/bin/env bash

set -ex

: ${INTEGRATIONTEST_SUITEFILES:=${INTEGRATIONTEST_SUITE_FILES}${ADDITIONAL_SUITEFILES+,$ADDITIONAL_SUITEFILES}}
: ${INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL:=1000}
: ${INTEGCB_LOCATION?"integcb location"}
: ${INTEGRATIONTEST_USER_ACCESSKEY:="Y3JuOmFsdHVzOmlhbTp1cy13ZXN0LTE6Y2xvdWRlcmE6dXNlcjptb2NrdXNlckB1bXMubW9jaw=="}
: ${INTEGRATIONTEST_USER_SECRETKEY:="nHkdxgZR0BaNHaSYM3ooS6rIlpV5E+k1CIkr+jFId2g="}
: ${INTEGRATIONTEST_TESTSUITE_CLEANUPONFAILURE:="true"}
: ${INTEGRATIONTEST_TESTSUITE_CLEANUP:="true"}

date
echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
cd $INTEGCB_LOCATION
./cbd kill
cd ..

date
echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
docker compose --compatibility down --remove-orphans

date
echo -e "\n\033[1;96m--- Copy mock infrastructure infrastructure-mock.p12 cert to certs dir\033[0m\n"
mkdir -p $INTEGCB_LOCATION/certs/trusted
cp ../mock-infrastructure/src/main/resources/keystore/infrastructure-mock.pem $INTEGCB_LOCATION/certs/trusted/infrastructure-mock.pem

date
echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION

unset HTTPS_PROXY
env

cbd_teardown_and_exit() {
  date
  echo -e "\n\033[1;96m--- ERROR: Failed to bring up all the necessary CBD services! Process is about to terminate!\033[0m\n"
  ./cbd kill
  docker compose --compatibility down --remove-orphans
  exit 1
}

cbd_services_sanity_check() {
  if [[ $RESULT -ne 0 ]]; then
    cbd_teardown_and_exit
  else
    local exited_containers=$(docker ps -f "name=cbreak" -f status=exited -f status=dead -f since=cbreak_nssdb-init-svc_1 -q)
    docker ps -f "name=cbreak" --format "table {{.ID}}\t{{.State}}\t{{.Names}}\t{{.Image}}"

    if [[ -n "$exited_containers" ]]; then
      echo -e "\n\033[1;96m--- ERROR: Only nssdb-init-svc is allowed to exit. However the following containers are exited/dead:\033[0m\n"
      docker ps -f "name=cbreak" -f status=exited -f status=dead --format "table {{.ID}}\t{{.State}}\t{{.Names}}\t{{.Image}}"
      cbd_teardown_and_exit
    else
      date
      echo -e "\n\033[1;96m--- INFO: All the necessary CBD services have been started successfully!\033[0m\n"
    fi
  fi
}

./cbd regenerate
./cbd start-wait traefik dev-gateway core-gateway commondb vault cloudbreak environment remote-environment periscope freeipa redbeams datalake externalized-compute haveged mock-infrastructure idbmms cluster-proxy cadence jumpgate-interop jumpgate-admin jumpgate-proxy thunderhead-mock
RESULT=$?
cbd_services_sanity_check

check_primary_key () {
    set +e
    DB_NAME="$1"
    docker exec -u postgres cbreak_commondb_1 psql -P pager=off -d "${DB_NAME}" -c "select tab.table_schema, tab.table_name \
        from information_schema.tables tab \
        left join information_schema.table_constraints tco \
                   on tab.table_schema = tco.table_schema \
                   and tab.table_name = tco.table_name \
                   and tco.constraint_type = 'PRIMARY KEY' \
        where tab.table_type = 'BASE TABLE' \
              and tab.table_schema='public' \
              and tco.constraint_name is null \
        order by table_schema, table_name;" | grep -q "(0 rows)"

    if [ $? -ne 0 ]; then
        set -e
        echo -e "\n\033[1;96m--- ERROR: There are tables in ${DB_NAME} without primary key. Process is about to terminate!\033[0m\n"
        ./cbd kill
        docker compose --compatibility down --remove-orphans
        exit 1
    fi
    set -e
}

if [ "${CB_TARGET_BRANCH}" == "master" ] && [ "${PRIMARYKEY_CHECK}" == "true" ]; then
    check_primary_key "cbdb"
    check_primary_key "periscopedb"
    check_primary_key "datalakedb"
    check_primary_key "environmentdb"
    check_primary_key "freeipadb"
    check_primary_key "redbeamsdb"
    check_primary_key "externalizedcomputedb"
fi

cd ..

#if [[ "$DOCKER_HOST" ]]; then
#    PUBLIC_IP=`echo $DOCKER_HOST | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}'`
#fi
#if [[ "$PUBLIC_IP" ]]; then
#    PUBLIC_IP=$PUBLIC_IP
#else
#    PUBLIC_IP=127.0.0.1
#fi
PUBLIC_IP=127.0.0.1

mkdir -p ./apidefinitions
curl -k http://${PUBLIC_IP}:8080/cb/api/openapi.json -o ./apidefinitions/cloudbreak.json
curl -k http://${PUBLIC_IP}:8088/environmentservice/api/openapi.json -o ./apidefinitions/environment.json
curl -k http://${PUBLIC_IP}:8092/remoteenvironmentservice/api/openapi.json -o ./apidefinitions/remote-environment.json
curl -k http://${PUBLIC_IP}:8090/freeipa/api/openapi.json -o ./apidefinitions/freeipa.json
curl -k http://${PUBLIC_IP}:8087/redbeams/api/openapi.json -o ./apidefinitions/redbeams.json
curl -k http://${PUBLIC_IP}:8086/dl/api/openapi.json -o ./apidefinitions/datalake.json
curl -k http://${PUBLIC_IP}:8085/as/api/openapi.json -o ./apidefinitions/autoscale.json
curl -k http://${PUBLIC_IP}:8091/externalizedcompute/api/openapi.json -o ./apidefinitions/externalizedcompute.json

date
echo -e "\n\033[1;96m--- Setting ACCESSKEY/SECRETKEY for test variables:\033[0m\n"
export INTEGRATIONTEST_USER_ACCESSKEY=$INTEGRATIONTEST_USER_ACCESSKEY
export INTEGRATIONTEST_USER_SECRETKEY=$INTEGRATIONTEST_USER_SECRETKEY

export INTEGRATIONTEST_SUITEFILES=$INTEGRATIONTEST_SUITEFILES
export INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL=$INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL

export INTEGRATIONTEST_CDL_HOST=$INTEGRATIONTEST_CDL_HOST
export INTEGRATIONTEST_CDL_PORT=$INTEGRATIONTEST_CDL_PORT

export INTEGRATIONTEST_UMS_HOST=$INTEGRATIONTEST_UMS_HOST
export INTEGRATIONTEST_UMS_PORT=$INTEGRATIONTEST_UMS_PORT
export INTEGRATIONTEST_UMS_ACCOUNTKEY=$INTEGRATIONTEST_UMS_ACCOUNTKEY
export INTEGRATIONTEST_UMS_DEPLOYMENTKEY=$INTEGRATIONTEST_UMS_DEPLOYMENTKEY
export INTEGRATIONTEST_UMS_JSONSECRET_VERSION=$INTEGRATIONTEST_UMS_JSONSECRET_VERSION
export INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH=$INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH
export INTEGRATIONTEST_UMS_JSONSECRET_NAME=$INTEGRATIONTEST_UMS_JSONSECRET_NAME

export INTEGRATIONTEST_TESTSUITE_CLEANUP=$INTEGRATIONTEST_TESTSUITE_CLEANUP
export INTEGRATIONTEST_TESTSUITE_CLEANUPONFAILURE=$INTEGRATIONTEST_TESTSUITE_CLEANUPONFAILURE

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
  export INTEGRATIONTEST_THREADCOUNT=16
  export INTEGRATIONTEST_CLOUDPROVIDER="MOCK"
fi

date
echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
rm -rf test-output

export DOCKER_CLIENT_TIMEOUT=120
export COMPOSE_HTTP_TIMEOUT=120

date
echo -e "\n\033[1;96m--- Env variables started with INTEGRATIONTEST :\033[0m\n"
env | grep -i INTEGRATIONTEST

date
env | grep -i INTEGRATIONTEST > integrationtest.properties

if [[ "$INTEGRATIONTEST_CLOUDPROVIDER" == "MOCK" ]]; then
  date
  echo -e "\n\033[1;96m--- Starting prometheus:\033[0m\n"
  docker compose --compatibility up -d prometheus
fi

date
echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
echo $INTEGRATIONTEST_SUITEFILES

set -o pipefail ; docker compose --compatibility up --remove-orphans --exit-code-from test test | tee test.out
echo -e "\n\033[1;96m--- Test finished\033[0m\n"

echo -e "\n\033[1;96m--- Collect docker stats:\033[0m\n"
if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
  sudo mkdir -p ./test-output
  sudo chmod -R a+rwx ./test-output
  sudo chmod -R a+rwx ./integcb/logs
  mkdir ./test-output/docker_stats
  docker stats --no-stream --format "{{ .NetIO }}" cbreak_commondb_1 > ./test-output/docker_stats/pg_stat_network_io.result;

  docker stats --no-stream --format "table {{ .Name }}\t{{ .Container }}\t{{ .MemUsage }}\t{{ .MemPerc }}\t{{ .CPUPerc }}\t{{ .NetIO }}\t{{ .BlockIO }}" > ./test-output/docker_stats/docker_stat.html
  docker exec cbreak_commondb_1 psql -U postgres --pset=pager=off -d cbdb -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;";
  docker exec cbreak_commondb_1 psql -U postgres --pset=pager=off -d cbdb -c "select * from pg_stat_statements;" --html > ./test-output/docker_stats/query_stat.html

  cp ./src/main/resources/pg_stats/pg_query_stat_template.html ./test-output/docker_stats/pg_query_stat_template.html

  #FIXME, might be better not to use in place sed
  sed -i '/<!-- CB_PG_STAT -->/r ./test-output/docker_stats/query_stat.html' ./test-output/docker_stats/pg_query_stat_template.html
  sed -i '/<!-- DOCKER_STAT_RESULT -->/r ./test-output/docker_stats/docker_stat.html' ./test-output/docker_stats/pg_query_stat_template.html

  mv ./test-output/docker_stats/pg_query_stat_template.html ./test-output/docker_stats/query_stat.html
fi