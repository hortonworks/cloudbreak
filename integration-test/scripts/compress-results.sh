#!/usr/bin/env bash

# Single compression stage for all integration-test artifacts (CB-33643).
# Everything is compressed exactly once here - in parallel via pigz when available - so the
# upload-artifact steps transport already-gzipped archives instead of many loose files.

if command -v pigz > /dev/null 2>&1; then
  COMPRESSOR=(pigz -1)
else
  COMPRESSOR=(gzip -1)
fi
echo "Using compressor: ${COMPRESSOR[*]}"

# tar exit code 1 means "some files changed as we read them" - logs can still be flushed after the
# container stop is issued - so tolerate it and only fail on anything else.
check_tar_exit () {
  local code=$1
  if [ "$code" != "0" ] && [ "$code" != "1" ]; then
    exit "$code"
  fi
}

compress_dir_if_exists () {
  local sourceDir=$1
  local targetArchive=$2

  if [[ ! -d $sourceDir ]]; then
    echo "Source dir '${sourceDir}' does not exist, skipping compress"
    return
  fi
  du -h "$sourceDir"
  echo "Compressing directory ${sourceDir} -> ${targetArchive}"
  set +e
  tar -cvf - "$sourceDir" | "${COMPRESSOR[@]}" > "$targetArchive"
  check_tar_exit "${PIPESTATUS[0]}"
  sudo chmod -R a+rwx "$sourceDir"
  rm -R "$sourceDir"
  set -e
  du -h "$targetArchive"
}

compress_files_if_exist () {
  local targetArchive=$1
  shift
  local existing=()
  for f in "$@"; do
    [[ -f $f ]] && existing+=("$f")
  done
  if [ ${#existing[@]} -eq 0 ]; then
    echo "No files found for '${targetArchive}', skipping compress"
    return
  fi
  echo "Compressing files -> ${targetArchive}: ${existing[*]}"
  set +e
  tar -cvf - "${existing[@]}" | "${COMPRESSOR[@]}" > "$targetArchive"
  check_tar_exit "${PIPESTATUS[0]}"
  rm -f "${existing[@]}"
  set -e
  du -h "$targetArchive"
}

start=`date +%s`
echo -e "\n\033[0;92m+++ INTEGRATION TEST RESULTS COMPRESS STARTED +++\n";

# Test result directories
compress_dir_if_exists suites_log suites.tar.gz
compress_dir_if_exists test-output test-output.tar.gz
compress_dir_if_exists integcb/logs integcb-logs.tar.gz

# Loose container logs collected by stop-containers.sh (gateway, cluster-proxy, thunderhead-mock)
compress_files_if_exist gateway-logs.tar.gz \
  dev-gateway.log core-gateway.log envoy.log thunderhead-mock.log \
  jumpgate-interop.log jumpgate-admin.log jumpgate-proxy.log

# Raw test console output
compress_files_if_exist test-console-output.tar.gz test.out

end=`date +%s`
echo -e "\n\033[0;92m+++ INTEGRATION TEST RESULTS COMPRESSED SUCCESSFULLY AND TOOK $((end-start)) SECONDS+++\n";
