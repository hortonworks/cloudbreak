#!/usr/bin/env bash

status_code=0

# Check integration test results
if grep -q 'skipped="0"' test-output/testng-results.xml && grep -q 'failed="0"' test-output/testng-results.xml;
    then echo -e "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n";
    else echo -e "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n"; status_code=1;
fi

# Check swagger validation results
if grep -q "is valid against swagger specification 2.0" swagger-validation-result.out;
    then echo -e "\n\033[0;92m+++ THE SWAGGER SPECIFICATION IS VALID +++\n";
    else echo -e "\033[0;91m--- !!! THE SWAGGER SPECIFICATION IS INVALID, CHECK \033[1;93mswagger-validation-result.out\033[0;91m FOR RESULTS !!! ---\n"; status_code=1;
fi

# Exit if there are failed tests
if [[ status_code -eq 1 ]];
    then exit 1;
fi