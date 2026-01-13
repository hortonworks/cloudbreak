#!/usr/bin/env bash
#please don't add this property to Jenkins
#this value changed 2023.04.06 from 4.8 to 4.9
#this value changed 2023.02.08 from 4.68 to 4.8 (added test for certificate swap test)
#this value changed 2023.02.02 from 4.6 to 4.65 (additional FreeIPA creation in MockSdxBlueprintLoadTests, MockSdxResizeTests, MockSdxRetryTests, MockSdxTests)
#this value changed 2022.11.24 from 4.5 to 4.6 (new test was added: DistroXUpgradeTests.testDistroXOsUpgradeByUpgradeSets)
#this value changed 2023.11.13 from 4.9 to 5.1 (new test was added: DistroXUpgradeTests.testDistroXBatchOsUpgrade)
#this value changed 2023.11.15 from 5.1 to 5.5 (new test was added: Cloudera runtime 7.3.0 introduced)
#this value changed 2026.01.22 from 5.5 to 5.7 (new test was added: MockSdxPemTests)
: ${INTEGRATIONTEST_MAX_PG_NETWORK_OUTPUT:="5.7GB"}

status_code=0

set -ex

# Check integration test results
date
if [[ ! -d test-output ]]; then
  echo -e "\033[0;91m--- !!! NO test-output DIRECTORY !!! ---\n";
  status_code=1;
else
  if grep -r '<failure ' test-output; then
    echo -e "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n";
    status_code=1;
  fi
  if grep -r '<skipped' test-output; then
    echo -e "\033[0;91m--- !!! INTEGRATION TEST SKIPPED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n";
    status_code=1;
  fi
fi

echo -e "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n";
    # Check integration test results
    date
    if [[ ! -d test-output ]]; then
        echo -e "\033[0;91m--- !!! NO test-output DIRECTORY !!! ---\n";
        status_code=1;
    else
        if grep -r '<failure ' test-output; then
          echo -e "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n";
          status_code=1;
        fi
        if grep -r '<skipped' test-output; then
          echo -e "\033[0;91m--- !!! INTEGRATION TEST SKIPPED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n";
          status_code=1;
        fi
    fi

if [[ $(find suites_log -maxdepth 1 -name '*.log' | wc -l) -lt 3 ]]; then
  echo -e "\033[0;91m--- !!! NO TEST HAS BEEN LAUNCHED !!! ---\n";
  status_code=1;
fi

if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
  docker run --rm \
    -v "$(pwd)"/scripts/analyse_postgres_stat.jsh:/opt/analyse_postgres_stat.jsh \
    -v "$(pwd)"/test-output/docker_stats/pg_stat_network_io.result:/tmp/pg_stat_network_io.result \
    --env INTEGRATION_TEST_MAX_POSTGRES_OUTPUT=${INTEGRATIONTEST_MAX_PG_NETWORK_OUTPUT} \
    docker-private.infra.cloudera.com/cloudera_base/ubi8/openjdk-21:1.18-3.1705519633 \
    jshell /opt/analyse_postgres_stat.jsh > ./test-output/docker_stats/pg_stat_network_io_analysed.result;

    pg_res=$(cat ./test-output/docker_stats/pg_stat_network_io_analysed.result);
    pg_ok="POSTGRES>> OK"
    if [[ "$pg_res" == $pg_ok* ]]; then
      echo -e "\n\033[0;92m+++ POSTGRES TRAFFIC IS BELOW ${max_pg_network_output} LIMIT. +++\n";
    else
      echo -e "\033[0;91m--- !!! POSTGRES TRAFIC CHECK FAILED: ${pg_res} !!! ---\n";
      status_code=1;
    fi
fi

# Exit if there are failed tests
if [[ status_code -eq 1 ]]; then
  echo -e "\033[0;91m--- !!! INTEGRATION TEST HAS BEEN FAILED !!! ---\n";
  exit 1;
fi

echo -e "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n";

