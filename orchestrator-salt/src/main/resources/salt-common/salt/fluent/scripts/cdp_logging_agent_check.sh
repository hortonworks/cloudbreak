#!/bin/sh

: ${LOGFILE_FOLDER:="/var/log/cdp-logging-agent-doctor"}
LIVENESS_THRESHOLD_SECONDS=86400 # 1 day in seconds
BUFFER_LIMIT_BYTES=100000 # ~100K in bytes
# increase this to make sure to override the script in case of diagnostics
# todo: find better solution - pass by cli option?
VERSION=1

readlinkf(){
  perl -MCwd -e 'print Cwd::abs_path shift' "$1";
}

if [ "$(uname -s)" = 'Linux' ]; then
  SCRIPT_LOCATION=$(readlink -f "$0")
else
  SCRIPT_LOCATION=$(readlinkf "$0")
fi

function print_help() {
  cat << EOF
   Usage: [<command>]
   commands:
     dump               dump details about logging worker processes for troubleshooting
     version            print version of the script
     help               print usage
EOF
}

function do_exit() {
  local code=$1
  local message=$2
  if [[ "$message" == "" ]]; then
    log "Exit code: $code"
  else
    log "Exit code: $code, --- STATUS MESSAGE --- $message --- STATUS MESSAGE ---"
  fi
  exit $code
}

function init_logfile() {
  local log_file_name=${1:?"usage: <log_file_name>"}
  mkdir -p $LOGFILE_FOLDER
  local timestamp=$(date +"%Y%m%d-%H%M%S")
  LOGFILE="$LOGFILE_FOLDER/${log_file_name}-${timestamp}.log"
  touch $LOGFILE
  chmod 600 $LOGFILE
  cleanup_old_logs "$log_file_name"
  log "The following log file will be used: $LOGFILE"
}

function cleanup_old_logs() {
  local log_file_name=${1:?"usage: <log_file_name>"}
  ls -1tr $LOGFILE_FOLDER/$log_file_name*.log | head -n -5 | xargs --no-run-if-empty rm
}

function log() {
  local timestamp=$(date +"%Y-%m-%dT%H:%M:%S.%3N%z")
  local debug=$2
  echo "$timestamp $1" >> $LOGFILE
  if [[ "$debug" == "" ]]; then
    echo "$1"
  fi
}

function get_fluentd_config_val() {
  local config_content=${1:?"usage: <config_content>"}
  local config_key=${2:?"usage: <config_key>"}
  echo "$config_content" | grep $config_key | head -n 1 | sed -e 's/^[[:space:]]*//' | cut -f2 -d' '
}

function get_base_path() {
  local full_ccloud_storage_path=${1:?"usage: <full_ccloud_storage_path>"}
  echo "$full_ccloud_storage_path" | tr -d '"' | awk -F'/%Y' '{print $1}'
}

function upload_to_cloud_storage() {
  local dump_file_name=${1:?"usage: <dump_file_name>"}
  local dump_base_filename=$(basename -- "$dump_file_name")
  if [[ ! -f /etc/cdp-logging-agent/output.conf ]]; then
    log "Logging agent config does not exist. Skip uploading data to cloud storage."
    return
  fi
  local timestamp_dump=$(date +"%Y-%m-%d")
  local dump_suffix="dumps/${timestamp_dump}"
  local fluent_out_config=$(cat /etc/cdp-logging-agent/output.conf)
  local s3_type=$(echo "$fluent_out_config" | grep "@type s3")
  local azure_type=$(echo "$fluent_out_config" | grep "@type azurestorage_gen2")
  local gcs_type=$(echo "$fluent_out_config" | grep "@type gcs")
  if [[ "$s3_type" != "" ]]; then
    log "Uploading dump to s3 ..."
    local s3_bucket=$(get_fluentd_config_val "$fluent_out_config" "s3_bucket")
    local s3_path_pattern=$(get_fluentd_config_val "$fluent_out_config" "%Y-%m-%d")
    local s3_base_path=$(get_base_path "$s3_path_pattern")
    local region=$(get_fluentd_config_val "$fluent_out_config" "region")
    log "Detected S3 configs: Bucket=$s3_bucket, BasePath=$s3_base_path, Region=$region"
    local target_location="$s3_base_path/$dump_suffix/"
    local additional_params=""
    if [[ "$region" != "" ]]; then
      additional_params="--region $region"
    fi
    cdp-telemetry storage s3 upload -e --bucket "$s3_bucket" --file "$dump_file_name" $additional_params --location "$target_location"
    local s3_upload_result="$?"
    if [[ "$s3_upload_result" == "0" ]]; then
      log "S3 upload COMPLETED: Bucket=$s3_bucket, Path=${target_location}${dump_base_filename}"
    else
      log "S3 upload failed with encryption, try without that parameter."
      cdp-telemetry storage s3 upload --bucket "$s3_bucket" --file "$dump_file_name" $additional_params --location "$target_location"
      local s3_upload_second_result="$?"
      if [[ "$s3_upload_second_result" == "0" ]]; then
        log "S3 upload COMPLETED: Bucket=$s3_bucket, Path=${target_location}${dump_base_filename}"
      else
        log "S3 upload FAILED: Bucket=$s3_bucket, Path=${target_location}${dump_base_filename}"
      fi
    fi
  elif [[ "$azure_type" != "" ]]; then
    local azure_storage_account=$(get_fluentd_config_val "$fluent_out_config" "azure_storage_account")
    local azure_container=$(get_fluentd_config_val "$fluent_out_config" "azure_container")
    local azure_path_pattern=$(get_fluentd_config_val "$fluent_out_config" "%Y-%m-%d")
    local azure_base_path=$(get_base_path "$azure_path_pattern")
    local target_location="$azure_base_path/$dump_suffix/"
    log "Detected ABFS configs: Account=$azure_storage_account, Container=$azure_container, BasePath=$azure_base_path"
    cdp-telemetry storage abfs --file "$dump_file_name" --location "${target_location}" --account "${azure_storage_account}" --container "${azure_container}"
    local abfs_upload_result="$?"
    if [[ "$abfs_upload_result" == "0" ]]; then
      log "ABFS upload COMPLETED: Account=$azure_storage_account, Container=$azure_container, Path=${target_location}${dump_base_filename}"
    else
      log "ABFS upload FAILED: Account=$azure_storage_account, Container=$azure_container, Path=${target_location}${dump_base_filename}"
    fi
  elif [[ "$gcs_type" != "" ]]; then
    local gcs_bucket=$(get_fluentd_config_val "$fluent_out_config" "bucket")
    local gcs_path_pattern=$(get_fluentd_config_val "$fluent_out_config" "%Y-%m-%d")
    local gcs_base_path=$(get_base_path "$gcs_path_pattern")
    local target_location="$gcs_base_path/$dump_suffix/"
    log "Detected GCS configs: Bucket=$gcs_bucket, BasePath=${gcs_base_path}"
    cdp-telemetry storage gcs upload --bucket "$gcs_bucket" --file "$dump_file_name" --location "${target_location}"
    local gcs_upload_result="$?"
    if [[ "$gcs_upload_result" == "0" ]]; then
      log "GCS upload COMPLETED: Bucket=$gcs_bucket, Path=${target_location}${dump_base_filename}"
    else
      log "GCS upload FAILED: Bucket=$gcs_bucket, Path=${target_location}${dump_base_filename}"
    fi
  else
    log "No configured cloud storage log shipping is detected. Skip uploading dump."
  fi
}

function create_dump_file_and_upload() {
  local dump_name=${1:?"usage: <dump_name>"}
  log "Compress /tmp/${dump_name}.tar.gz file ..."
  ( cd /tmp && tar -czvf "${dump_name}.tar.gz" "${dump_name}" && rm -r "${dump_name}" )
  log "Compression complete. Cleanup old dump collections ..."
  ls -1tr /tmp/cdp-logging-dump*.tar.gz | head -n -3 | xargs --no-run-if-empty rm
  upload_to_cloud_storage "/tmp/${dump_name}.tar.gz"
  log "LOCAL DUMP FINISHED"
}

function dump() {
  local timestamp_for_folder=$(date +"%Y%m%d-%H%M%S")
  local hostname_short=$(hostname)
  local dump_name="cdp-logging-dump_${hostname_short}-${timestamp_for_folder}"
  local dump_folder="/tmp/${dump_name}"
  local logging_agent_pids_resp=$(get_logging_agent_worker_pids)
  if [[ "${logging_agent_pids_resp}" == "" ]]; then
    log "No any logging agents are running. Skip worker dump."
  else
    if [[ ! -d "${dump_folder}" ]]; then
      log "Creating dump folder: ${dump_folder}"
      mkdir -p "${dump_folder}"
    fi
    local worker=0
    log "Execute command du -d ..."
    local disk_check_out=$(du -h /var/log/cdp-logging-agent/*)
    log "Execute command: stat ..."
    local access_check_out=$(stat /var/log/cdp-logging-agent/*)
    log "Gather process details dump for workers..."
    local proc_dump=$(ps aux | grep cdp-logging-agent | awk 'NR>1 {$5=int($5/1024)"M (RSS)"; $6=int($6/1024)"M (VSZ)";}{ print;}' | grep "under-supervisor")

    echo "$disk_check_out" >> "${dump_folder}/du.txt"
    echo "$access_check_out" >> "${dump_folder}/stat.txt"
    echo "$proc_dump" >> "${dump_folder}/proc.txt"
    for logging_agent_pid in $logging_agent_pids_resp; do
      ((worker=worker+1))
      local thread_dump_file="/tmp/sigdump-${logging_agent_pid}.log"
      log "Thread dump logging agent worker #${worker} to ${thread_dump_file}"
      kill -CONT $logging_agent_pid
      local thread_dump_content=$(cat "$thread_dump_file")
      log "START OF WORKER #${worker} (Pid: ${logging_agent_pid}) THREAD DUMP"
      log "END OF WORKER #${worker} (Pid: ${logging_agent_pid}) THREAD DUMP"
      local timestamp=$(date +"%Y%m%d-%H%M%S")
      local thread_dump_log_file="${dump_folder}/worker-thread-dump-${worker}-${logging_agent_pid}-${timestamp}.txt"
      log "Copying thread dump to $thread_dump_log_file"
      cp -r $thread_dump_file "${dump_folder}/worker-thread-dump-${worker}-${logging_agent_pid}-${timestamp}.txt"
      log "Collect addition file descriptors..."
      local fd_output=$(ls -la /proc/$logging_agent_pid/fd)
      log "Gather file descriptor data for #${worker} (Pid: ${logging_agent_pid})"
      echo "${fd_output}" >> "${dump_folder}/fd-${worker}-${logging_agent_pid}.txt"
    done
    create_dump_file_and_upload $dump_name
  fi
}

function get_logging_agent_worker_pids() {
  ps aux | grep cdp-logging-agent | grep under-supervisor | awk '{print $2}'
}

function setup_workdir() {
  if [[ "$WORKING_DIR" == "" ]]; then
      WORKING_DIR="/tmp"
  elif [[ ! -d "$WORKING_DIR" ]]; then
    log "Working directory does not exists. Creating it..."
    mkdir -p "$WORKING_DIR"
  fi
}

function run_operation() {
  local operation=${1:?"usage: <operation>"}
  if [[ "${operation}" == "version" ]]; then
    echo "${VERSION}"
    return
  fi
  if [[ "${operation}" == "dump" ]]; then
    init_logfile "dump"
    setup_workdir
    dump
  fi
}

function main() {
  command="$1"
  case $command in
   "dump")
      run_operation "dump"
    ;;
    "version")
      run_operation "version"
    ;;
    "help")
      print_help
    ;;
   *)
    echo "Available commands: (dump | version | help)"
   ;;
   esac
}

main ${1+"$@"}