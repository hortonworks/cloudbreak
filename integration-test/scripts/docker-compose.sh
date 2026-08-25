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
docker compose stop
docker compose rm -f
cd ..

date
echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
docker compose down --remove-orphans

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
  docker compose down --remove-orphans
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

less Profile
./cbd regenerate

# mock-infrastructure -- the cloud backend EVERY test's operations funnel through -- was the
# suite-wide throughput ceiling: docker stats showed it pinned flat at ~1 core (its deployer-default
# cgroup cap) while every other service burst to many cores, serialising the whole suite. Its cap is
# lifted via the CPUS_FOR_MOCK_INFRASTRUCTURE / MEMORY_FOR_MOCK_INFRASTRUCTURE Profile vars (appended
# for this 8xlarge job in integration-test-steps_integration_test.sh), which cbd feeds into the compose
# template so the container is simply born with the right cgroup -- no compose override, no live mutation.
./cbd start-wait traefik dev-gateway core-gateway envoy commondb vault cloudbreak environment remote-environment periscope freeipa redbeams datalake externalized-compute haveged mock-infrastructure idbmms cadence jumpgate-interop jumpgate-admin jumpgate-proxy thunderhead-mock
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
        docker compose down --remove-orphans
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
PUBLIC_IP=localhost

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
  # 24 parallel methods. DO NOT raise this without fixing the shared bottleneck first: raising it to
  # 40 caused CONGESTION COLLAPSE, not speedup. Measured from testng-results.xml, the SAME 226 tests
  # took +91% cumulative time at 40 threads vs 24 (e.g. testScaleDownAndUp 546s->1397s; 93 of 103
  # heavy tests >15% slower), pushing the phase 25.6min->30.4min and failures 1->17 (flow poll
  # timeouts from congestion). The shared bottleneck was mock-infrastructure, capped at 1 CPU core by
  # the deployer default; that cap is now lifted via the CPUS_FOR_MOCK_INFRASTRUCTURE Profile var (see
  # the start-wait section above), which dropped per-test durations enough for 24 to be stable. Threads
  # could go higher now, but only until a shared resource starts saturating again -- verify first.
  export INTEGRATIONTEST_THREADCOUNT=24
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
  docker compose up -d prometheus
fi

date
echo -e "\n\033[1;96m--- Tests to run:\033[0m\n"
echo $INTEGRATIONTEST_SUITEFILES

# --- Start background per-container resource sampler (CB-33589 IT perf diagnostics) ---
# Grafana's Kubernetes pod view only sees the `runner`/`dind` containers, NOT the docker-compose
# services nested inside dind, so it cannot show whether cloudbreak is CPU-starved. Sample
# `docker stats` here -- inside the job, where the nested containers ARE visible -- throughout the
# whole test run to capture PEAK per-container CPU/memory. The teardown snapshot alone is idle.
DOCKER_STATS_SAMPLER_PID=""
if [[ "$AWS" != true ]] && [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]]; then
  mkdir -p ./test-output/docker_stats
  echo "timestamp,name,cpu_perc,mem_usage,mem_perc,net_io,block_io" > ./test-output/docker_stats/docker_stats_timeseries.csv
  ( while true; do
      ts="$(date -u +%FT%TZ)"
      docker stats --no-stream --format "{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}},{{.BlockIO}}" 2>/dev/null \
        | sed "s#^#${ts},#"
      sleep 15
    done ) >> ./test-output/docker_stats/docker_stats_timeseries.csv 2>&1 &
  DOCKER_STATS_SAMPLER_PID=$!
  echo "Started docker stats sampler (pid=${DOCKER_STATS_SAMPLER_PID}, interval=15s) -> test-output/docker_stats/docker_stats_timeseries.csv"
fi

set -o pipefail ; docker compose up --remove-orphans --exit-code-from test test > test.out 2>&1
echo -e "\n\033[1;96m--- Test output would be too long, stored in the \033[1;93mtest.out\033[1;96m file \033[0m\n"

echo -e "\n\033[1;96m--- Test finished\033[0m\n"

# Stop the background resource sampler started before the test run.
if [[ -n "${DOCKER_STATS_SAMPLER_PID}" ]]; then
  kill "${DOCKER_STATS_SAMPLER_PID}" 2>/dev/null || true
  wait "${DOCKER_STATS_SAMPLER_PID}" 2>/dev/null || true
  echo "Stopped docker stats sampler (pid=${DOCKER_STATS_SAMPLER_PID})"
fi

echo "--- Post-test cbreak container status ---"
docker ps -a -f "name=cbreak" \
  --format "table {{.Names}}\t{{.Status}}\t{{.State}}\t{{.Image}}"
docker ps -a -f "name=cbreak" -f status=exited -f status=dead \
  --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"

echo -e "\n\033[1;96m--- Collect docker stats:\033[0m\n"
if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
  sudo mkdir -p ./test-output
  sudo chmod -R a+rwx ./test-output
  sudo chmod -R a+rwx ./integcb/logs
  mkdir -p ./test-output/docker_stats
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