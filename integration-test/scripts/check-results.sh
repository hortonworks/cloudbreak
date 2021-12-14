#!/usr/bin/env bash

: ${INTEGRATIONTEST_MAX_PG_NETWORK_OUTPUT:="7.5GB"}

status_code=0

set -ex

if [[ "$CIRCLECI" ]]; then

    # Checking integration test results
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

    if [[ -z $ghprbPullId ]]; then
      # Checking timed out tests
      declare executed_tests=$(find suites_log -maxdepth 1 -name '*.test*.log' | wc -l)
      declare generated_results=$(find . -maxdepth 1 -name 'resource_names_test*.json' | wc -l)
      declare -i diff=$executed_tests-$generated_results
      declare -a suite_logs
      declare -a resource_files
      declare -a timed_out_tests
      if [[ $diff -gt 0 ]]; then
          while IFS=  read -r -d $'\0'; do
              REPLY=${REPLY#*.}; REPLY=${REPLY%-*}
              suite_logs+=("$REPLY")
          done < <(find suites_log -type f -name '*.test*.log' -print0)
          while IFS=  read -r -d $'\0'; do
              REPLY=${REPLY#*resource_names_}; REPLY=${REPLY%.json*}
              resource_files+=("$REPLY")
          done < <(find . -type f -name 'resource_names_test*.json' -print0)

          for suite_log in "${suite_logs[@]}"; do
            if [[ ! ${resource_files[*]} =~ $suite_log ]]; then
              timed_out_tests+=("$suite_log")
            fi
          done

          if [[ ! -z "${timed_out_tests[*]}" ]]; then
            echo -e "\033[0;91m--- !!! ["${timed_out_tests[*]}"] TEST HAS BEEN TIMEDOUT !!! ---\n";
            status_code=1;
          fi
      fi
    fi

    if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
      docker run --rm \
       -v "$(pwd)"/scripts/analyse_postgres_stat.jsh:/opt/analyse_postgres_stat.jsh \
       -v "$(pwd)"/test-output/docker_stats/pg_stat_network_io.result:/tmp/pg_stat_network_io.result \
       --env INTEGRATION_TEST_MAX_POSTGRES_OUTPUT=${INTEGRATIONTEST_MAX_PG_NETWORK_OUTPUT} \
       docker-private.infra.cloudera.com/cloudera_thirdparty/openjdk/openjdk:11-jdk \
       jshell /opt/analyse_postgres_stat.jsh > ./test-output/docker_stats/pg_stat_network_io_analysed.result;

      pg_res=$(cat ./test-output/docker_stats/pg_stat_network_io_analysed.result);
      if [[ "$pg_res" == "POSTGRES>> OK" ]]; then
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
fi
