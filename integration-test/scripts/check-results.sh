#!/usr/bin/env bash

if grep -q 'skipped="0".*failed="0"' test-output/testng-results.xml;
    then echo -e "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n";
    else echo -e "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n"; exit 1;
fi
