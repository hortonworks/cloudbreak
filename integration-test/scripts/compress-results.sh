#!/usr/bin/env bash

compress_if_exists () {
  sourceDir=$1
  targetArchive=$2

  if [[ ! -d $sourceDir ]]; then
    echo "Source dir '$sourceDir' does not exists, skipping compress"
  else
    du -h $sourceDir
    echo "Compressing $sourceDir directory"
    #special error handling is needed since logs might still be flushed even after docker container stop is issued
    #tar gz file changed as we read it ignore
    set +e
    GZIP=-9 tar -cvzf "$targetArchive" "$sourceDir"
    exitcode=$?
    if [ "$exitcode" != "1" ] && [ "$exitcode" != "0" ]; then
        exit $exitcode
    fi
    sudo chmod -R a+rwx "$sourceDir"
    rm -R "$sourceDir"
    set -e
    du -h "$targetArchive"
  fi
}

start=`date +%s`
echo -e "\n\033[0;92m+++ INTEGRATION TEST RESULTS COMPRESS STARTED +++\n";

# Check integration test results
compress_if_exists suites_log suites.tar.gz
compress_if_exists test-output test-output.tar.gz
compress_if_exists integcb/logs integcb-logs.tar.gz

end=`date +%s`
echo -e "\n\033[0;92m+++ INTEGRATION TEST RESULTS COMPRESSED SUCCESSFULLY AND TOOK $((end-start)) SECONDS+++\n";