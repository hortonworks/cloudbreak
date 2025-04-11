#!/usr/bin/env bash


set -ex

: ${INTEGRATIONTEST_SUITEFILES:=${INTEGRATIONTEST_SUITE_FILES}${ADDITIONAL_SUITEFILES+,$ADDITIONAL_SUITEFILES}}
: ${INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL:=1000}
: ${INTEGCB_LOCATION?"integcb location"}
: ${INTEGRATIONTEST_USER_ACCESSKEY:="Y3JuOmFsdHVzOmlhbTp1cy13ZXN0LTE6Y2xvdWRlcmE6dXNlcjptb2NrdXNlckB1bXMubW9jaw=="}
: ${INTEGRATIONTEST_USER_SECRETKEY:="nHkdxgZR0BaNHaSYM3ooS6rIlpV5E+k1CIkr+jFId2g="}

date
echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
docker compose --compatibility down --remove-orphans

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

cd $INTEGCB_LOCATION
./cbd regenerate
echo $RESTARTABLE_SERVICES | xargs ./cbd start-wait
RESULT=$?
cbd_services_sanity_check
cd ..

PUBLIC_IP=127.0.0.1

date
echo -e "\n\033[1;96m--- Env variables started with INTEGRATIONTEST :\033[0m\n"
env | grep -i INTEGRATIONTEST
date
while read line; do
  export $line
done < integrationtest.properties

date
echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
echo $INTEGRATIONTEST_SUITEFILES

date
echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
rm -rf test-output

export DOCKER_CLIENT_TIMEOUT=120
export COMPOSE_HTTP_TIMEOUT=120

set -o pipefail ; docker compose --compatibility up --remove-orphans --exit-code-from test test | tee test_2.out
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