#!/usr/bin/env bash

: ${INTEGRATIONTEST_MAX_PG_NETWORK_OUTPUT:="16GB"}

status_code=0

if [[ "$CIRCLECI" ]]; then

    # Check integration test results
    date
    if [[ ! -d test-output ]]; then
        echo -e "\033[0;91m--- !!! NO test-output DIRECTORY !!! ---\n"; status_code=1;
    else
        grep -r '<failure ' test-output
        if [[ "$?" -eq 0 ]]; then
          echo -e "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n"; status_code=1;
        fi
        grep -r '<skipped' test-output
        if [[ "$?" -eq 0 ]]; then
          echo -e "\033[0;91m--- !!! INTEGRATION TEST SKIPPED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n"; status_code=1;
        fi
    fi

    # Check swagger validation results
    if ! grep -q "is valid against swagger specification 2.0" swagger-validation-result.out; then
        echo -e "\033[0;91m--- !!! THE SWAGGER SPECIFICATION IS INVALID, CHECK \033[1;93mswagger-validation-result.out\033[0;91m FOR RESULTS !!! ---\n"; status_code=1;
    fi

    if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
      docker run --rm \
       -v "$(pwd)"/scripts/analyse_postgres_stat.jsh:/opt/analyse_postgres_stat.jsh \
       -v "$(pwd)"/test-output/docker_stats/pg_stat_network_io.result:/tmp/pg_stat_network_io.result \
       --env INTEGRATION_TEST_MAX_POSTGRES_OUTPUT=${INTEGRATIONTEST_MAX_PG_NETWORK_OUTPUT} \
       openjdk:11-jdk \
       jshell /opt/analyse_postgres_stat.jsh > ./test-output/docker_stats/pg_stat_network_io_analysed.result;

      pg_res=$(cat ./test-output/docker_stats/pg_stat_network_io_analysed.result);
      if [[ "$pg_res" == "POSTGRES>> OK" ]]; then
        echo -e "\n\033[0;92m+++ Postgres traffic is below ${max_pg_network_output} limit.";
      else
        echo -e "\033[0;91m--- !!! Postgres trafic check failed: ${pg_res}"; status_code=1;
      fi
    fi

    # Exit if there are failed tests
    if [[ status_code -eq 1 ]];
        then exit 1;
    fi

    echo -e "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n";
    echo -e "\n\033[0;92m+++ THE SWAGGER SPECIFICATION IS VALID +++\n";
fi
