#!/usr/bin/env bash

status_code=0

compress_if_exists () {
  sourceDir=$1
  targetArchive=$2

  if [[ ! -d $sourceDir ]]; then
    echo "Source dir '$sourceDir' does not exists, skipping compress"
  else
    du -h $sourceDir
    echo "Compressing $sourceDir directory"
    GZIP=-9 tar -cvzf "$targetArchive" "$sourceDir" #&& rm -R "$sourceDir"
    du -h "$targetArchive"
  fi
}

set -ex

start=`date +%s`
echo -e "\n\033[0;92m+++ INTEGRATION TEST RESULTS COMPRESS STARTED +++\n";

# Check integration test results
compress_if_exists suites_log suites.tar.gz
compress_if_exists integcb/logs integcb-logs.tar.gz

end=`date +%s`
echo -e "\n\033[0;92m+++ INTEGRATION TEST RESULTS COMPRESSED SUCCESSFULLY AND TOOK $((end-start)) SECONDS+++\n";