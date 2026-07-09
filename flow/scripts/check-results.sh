#!/usr/bin/env bash

status_code=0

set -ex

echo -e "\n\033[1;96m--- Check component test results\033[0m\n"
date
if [[ ! -d build/test-results/test ]]; then
    echo -e "\033[0;91m--- !!! NO test-results DIRECTORY !!! ---\033[0m\n";
    status_code=1;
else
    if grep -r '<failure ' build/test-results/test; then
      echo -e "\033[0;91m--- !!! COMPONENT TEST FAILED, CHECK \033[1;93mbuild/test-results/test\033[0;91m DIR FOR RESULTS !!! ---\033[0m\n";
      status_code=1;
      echo -e "\033[0;91m--- LIST OF FAILED CLASSES/TESTCASES: ---\033[0m\n"
      awk '
        /<testcase / {
          if (match($0, /classname="[^"]+"/)) {
            class_name = substr($0, RSTART + 11, RLENGTH - 12)
          }
          if (match($0, /name="[^"]+"/)) {
            test_name = substr($0, RSTART + 6, RLENGTH - 7)
          }
        }
        /<failure/ {
          if (class_name != "" && test_name != "") {
            printf "\033[31m%s.%s\033[0m\n", class_name, test_name
          }
        }
      ' build/test-results/test/TEST-*.xml
    fi
    if grep -rq '<skipped' build/test-results/test; then
      echo -e "\033[1;93m--- Skipped tests present (e.g. @Disabled); not treated as failure when Gradle succeeded ---\033[0m\n"
      awk '
        /<testcase / {
          if (match($0, /classname="[^"]+"/)) {
            class_name = substr($0, RSTART + 11, RLENGTH - 12)
          }
          if (match($0, /name="[^"]+"/)) {
            test_name = substr($0, RSTART + 6, RLENGTH - 7)
          }
        }
        /<skipped/ {
          if (class_name != "" && test_name != "") {
            printf "\033[33m%s.%s\033[0m\n", class_name, test_name
          }
        }
      ' build/test-results/test/TEST-*.xml
    fi
fi

echo -e "\033[1;96mHtml result of test run: \033[1;93m/build/reports/tests/test/index.html\033[0m\n"
echo -e "\033[1;96mFlow DB exports: \033[1;93m/build/flowlog.html\033[0m, \033[1;93m/build/flowchainlog.html\033[0m, \033[1;93m/build/flowoperationstats.html\033[0m\n"

if [[ $(find build/test-results/test -maxdepth 1 -name 'TEST-*.xml' | wc -l) -lt 1 ]]; then
  echo -e "\033[0;91m--- !!! NO TEST HAS BEEN LAUNCHED !!! ---\033[0m\n";
  status_code=1;
fi

if [[ $status_code -eq 1 ]]; then
  echo -e "\033[0;91m--- !!! COMPONENT TEST HAS BEEN FAILED !!! ---\033[0m\n";
  exit 1;
fi

echo -e "\n\033[0;92m+++ COMPONENT TEST SUCCESSFULLY FINISHED +++\033[0m\n";
