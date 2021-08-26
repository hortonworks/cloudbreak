#!/bin/sh

EXPECTED_MIN_VERSION="0.4.14"
# ~300MB
MAX_FILE_SIZE=314572800
ONE_GB_FILE_SIZE=1073741824

typeset -A LOGS_TO_CHECK=([syslog]=/var/log/messages [salt_master]=/var/log/salt/master [salt_minion]=/var/log/salt/minion [salt_api]=/var/log/salt/api);

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
   Usage: [<command>] [<arguments with flags>]
   commands:
     master-check         Run preflight checks from master node on files before executing any diagnostics - also it sends commands to all available minions.
   upgrade command arguments:
     -h, --hosts          Salt nodes to target
EOF
}

function do_exit() {
  local code=$1
  local message=$2
  EXITING="true"
  if [[ "$message" == "" ]]; then
    log "Exit code: $code"
  else
    log "Exit code: $code, Status message: $message"
  fi
  exit $code
}

function log() {
  if [[ "$OPERATION_NAME" == "master-check" || "$EXITING" == "true" ]]; then
    local timestamp=$(date +"%Y-%m-%dT%H:%M:%S.%3N%z")
    echo "$timestamp $1"
  fi
}

function init_salt_prefix() {
  SALT_BIN_PREFIX=$(find /opt -maxdepth 1 -type d -iname "salt_*" | xargs -I{} echo "{}/bin")
  if [[ "$SALT_BIN_PREFIX" == "" ]]; then
    SALT_BIN_PREFIX="/opt/salt_*/bin"
  fi
}

function run_command() {
  local cmd=${1:?"usage: <command>"}
  log "The following command will be executed: $1"
  eval $1
}

function is_cdp_telemetry_installed() {
  local installed=$(rpm -q "cdp-telemetry" 2>&1 >/dev/null; echo $?)
  echo "$installed"
}

function get_telemetry_binary_version() {
  local installed=$(is_cdp_telemetry_installed)
  if [[ "$installed" == "0" ]]; then
    local telemetry_tool_version=$(rpm -q --queryformat '%-{VERSION}' "cdp-telemetry")
    echo "$telemetry_tool_version"
  else
    echo "-1"
  fi
}

function get_file_size() {
  local log_file=$1
  test -f $log_file && stat --printf="%s" $log_file || echo -n 0
}

function version_lt() {
  local version_to_compare=${1:?"usage: <version to compare>"}
  local current_version=${2:?"usage: <current version>"}
  local lower_val=$(printf "$version_to_compare\n$current_version" | sort -V | head -1)
  if [[ "$lower_val" == "$version_to_compare" ]]; then
    echo "0"
  elif [[ "$lower_val" == "$current_version" ]]; then
    echo "1"
  else
    log "Invalid state for version compare (operand 1: $version_to_compare, operand 2: $current_version)"
    echo "1"
  fi
}

function check_minion_file_size() {
  local minion_node=$1
  local log_size=$2
  local file=$3
  local log_s=$(expr $log_size + 0)
  local max_log_s=$(expr $ONE_GB_FILE_SIZE + 0)
  if [ "$log_s" -gt "$max_log_s" ]; then
    echo "$minion_node" >> $file
  fi
}

function master_check() {
  log "Running master preflight diagnostics check ..."
  telemetry_version=$(get_telemetry_binary_version)

  local do_not_return_start="Minion did not return"
  log "Gather pkg versions for cdp-telemetry"
  run_command "$SALT_BIN_PREFIX/salt '*' pkg.version cdp-telemetry --out-indent=-1 --out=json --out-file=$WORKING_DIR/preflight_cdp_telemetry_versions.json"

  rm -rf $WORKING_DIR/preflight_not_responding.txt
  rm -rf $WORKING_DIR/preflight_do_nothing.txt
  rm -rf $WORKING_DIR/preflight_old_versions.txt

  for row in $(cat "$WORKING_DIR/preflight_cdp_telemetry_versions.json" | jq -r 'to_entries | . []|=.key+":"+.value | @base64'); do
    local decoded_row=$(echo "${row}" | base64 -d | jq .[] | tr -d '"')
    local salt_minion_node=$(echo "${decoded_row}" | cut -d ':' -f1)
    local full_value=$(echo "${decoded_row}" | cut -d ':' -f2)
    local binary_version=$(echo "${full_value}" | cut -d '-' -f1)
    log "Found node/version pair: ${salt_minion_node} - ${binary_version}"
    local hosts_array=""
    if [[ "$HOSTS_FILTER" != "" ]]; then
      hosts_array=($(echo $HOSTS_FILTER | tr "," "\n"))
    fi
    if [[ "${full_value}" == *do_not_return_start* ]];then
      log "Salt minion with name '${salt_minion_node}' is not responding."
      echo "${salt_minion_node}" >> $WORKING_DIR/preflight_not_responding.txt
    elif [[ "${binary_version}" == "${EXPECTED_MIN_VERSION}" || "${binary_version}" == *"SNAPSHOT"* ]]; then
      log "Salt minion with name '${salt_minion_node}' is up to date (version: ${binary_version})."
      echo "${salt_minion_node}" >> $WORKING_DIR/preflight_do_nothing.txt
    elif [[ "${binary_version}" != "" ]]; then
      local lower_val_in_binary=$(version_lt $EXPECTED_MIN_VERSION $binary_version )
      if [[ "$lower_val_in_binary" != "0" ]]; then
        local filtered="false"
        if [[ "$hosts_array" != "" ]]; then
          for filtered_host in "${hosts_array[@]}"; do
            if [[ "${salt_minion_node}" == "${filtered_host}" ]]; then
              log "${filtered_host} will be included in the check."
              filtered="true"
            fi
          done
        else
          filtered="true"
        fi
        if [[ "$filtered" == "true" ]]; then
          log "Salt minion with name '${salt_minion_node}' needs an upgrade for cdp-telemetry (at least version: ${EXPECTED_MIN_VERSION})."
          echo "${salt_minion_node}" >> $WORKING_DIR/preflight_old_versions.txt
        else
          log "Salt minion with name '${salt_minion_node}' will be skipped from the diagnostics check."
          echo "${salt_minion_node}" >> $WORKING_DIR/preflight_do_nothing.txt
        fi
      fi
    fi
  done

  local distribution_folder="/srv/salt/distribution"
  log "Local cdp-telemetry version: ${telemetry_version}"
  log "Creating $distribution_folder if does not exists."
  mkdir -p $distribution_folder
  SRC_DIR="$( dirname "$SCRIPT_LOCATION" )"
  minion_check_script=$SRC_DIR/filecollector_minion_check.py
  if [[ ! -f "$minion_check_script" ]]; then
    local full_error_msg=$(cat << EOF
PreFlight diagnostics check FAILED
The preflight check script is missing, could be upload failed onto the salt master node.
EOF
    )
    do_exit 1 "$full_error_msg"
  fi
  log "Copying $minion_check_script into $distribution_folder."
  cp -r $minion_check_script $distribution_folder/

  local preflight_check_targets=""
  if [[ -f "$WORKING_DIR/preflight_old_versions.txt" ]];then
    preflight_check_targets=$(cat "$WORKING_DIR/preflight_old_versions.txt" | paste -sd "," -)
  fi

  if [[ "$preflight_check_targets" == "" ]]; then
    log "Targets are empty for diagnostics preflight checks. Skip running any salt operations on them."
    do_exit 0
  else
    log "There are old telemetry versions on the cluster. Upgrade recommended filecollector cdp-telemetry version in the filecollector config"
    if [[ -f "/srv/salt/filecollector/init.sls" ]]; then
      sed -i "s/.*set cdp_telemetry_version.*/{% set cdp_telemetry_version = '${EXPECTED_MIN_VERSION}' %}/" /srv/salt/filecollector/init.sls
    fi
  fi

  run_command "$SALT_BIN_PREFIX/salt -L $preflight_check_targets cmd.run 'rm -rf /tmp/filecollector_minion_check.py' timeout=60"
  run_command "$SALT_BIN_PREFIX/salt -L $preflight_check_targets cp.get_file salt:///distribution/filecollector_minion_check.py /tmp/filecollector_minion_check.py"
  run_command "$SALT_BIN_PREFIX/salt -L $preflight_check_targets cmd.run 'chmod 750 /tmp/filecollector_minion_check.py && python3 /tmp/filecollector_minion_check.py' --out-indent=-1 --out=json --out-file=$WORKING_DIR/preflight_check_out.json timeout=60"

  for key in "${!LOGS_TO_CHECK[@]}"; do
    rm -rf "$WORKING_DIR/preflight_${key}_log_errors.txt"
  done

  rm -rf "$WORKING_DIR/preflight_no_ram.txt"

  for row in $(cat "$WORKING_DIR/preflight_check_out.json" | jq -r 'to_entries | . []|=.key+":"+.value | @base64'); do
    local decoded_row=$(echo "${row}" | base64 -d | jq .[] | sed -r 's/^"|"$//g')
    local salt_minion_node=$(echo "${decoded_row}" | cut -d ':' -f1)
    local full_value=$(echo "${decoded_row}" | cut -d ':' -f2- | tr -d '\\')
    if [[ "$full_value" == "{}" ]]; then
      log "Salt minion with name '${salt_minion_node}' has an empty response, probably it does not have filecollector."
    elif [[ "$full_value" == *do_not_return_start* ]]; then
      log "Salt minion with name '${salt_minion_node}' is not responding."
    else
      for key in "${!LOGS_TO_CHECK[@]}"; do
        local log_size=$(echo "$full_value" | jq ".${key}")
        check_minion_file_size $salt_minion_node $log_size "$WORKING_DIR/preflight_${key}_log_errors.txt"
      done
      local has_enough_memory_val=$(echo "$full_value" | jq ".hasEnoughFreeMemory")
      if [[ "$has_enough_memory_val" == "false" ]]; then
        echo $salt_minion_node >> "$WORKING_DIR/preflight_no_ram.txt"
      fi
      local skip_labels_for_node=$(echo "$full_value" | jq ".skipLabels")
      if [[ "$skip_labels_for_node" != "" ]]; then
        log "Warning: Skipped labels for node '$salt_minion_node': $skip_labels_for_node"
      fi
    fi
  done
  if [[ -f "$WORKING_DIR/preflight_no_ram.txt" ]];then
    nodes_memory_issues=$(cat "$WORKING_DIR/preflight_no_ram.txt" | paste -sd "," -)
    if [[ "$nodes_memory_issues" != "" ]]; then
      log "-- RAM ISSUES --- The following nodes can have missing logs from the bundle (as the lack of RAM to process them): $nodes_memory_issues -- RAM ISSUES ---"
    fi
  fi

  local error_msg=""
  for key in "${!LOGS_TO_CHECK[@]}"; do
    local error_nodes=""
    if [[ -f "$WORKING_DIR/preflight_${key}_log_errors.txt" ]]; then
      error_nodes=$(cat "$WORKING_DIR/preflight_${key}_log_errors.txt" | paste -sd "," -)
      if [[ "$error_nodes" != "" ]];then
        error_msg=$(echo -e "$error_msg \n[${LOGS_TO_CHECK[$key]}]: $error_nodes")
      fi
    fi
  done

  # cleanup
  rm -rf /srv/salt/distribution

  if [[ "$error_msg" != "" ]];then
    local full_error_msg=$(cat << EOF
PreFlight diagnostics check FAILED
Diagnostics log collection has been interrupted in order to avoid memory pressure on VM nodes that could shut down the machines, as some files are too large to process (300 MB > per log file).
Please upgrade cdp-telemetry binary to workaround the issue (min version $EXPECTED_MIN_VERSION) or filter out affected hosts (by CDP cli / UI) or labels (by only CDP cli).
The following logs/nodes are affected: $error_msg

You can upgrade the cdp-telemetry binary by using --update-package option by CDP cli (for environment/datalake/datahub collect-XX-diagnostics commands). But note that requires network access for https://cloudera-service-delivery-cache.s3.amazonaws.com (on the VM nodes).
Alternatively, the binary can be updated manually by the following steps (e.g. for version $EXPECTED_MIN_VERSION):
1. Download rpm from https://cloudera-service-delivery-cache.s3.amazonaws.com/telemetry/cdp-telemetry/cdp_telemetry-$EXPECTED_MIN_VERSION.x86_64.rpm
2. Scp the rpm file to a salt-master node. (e.g. into /home/cloudbreak folder)
3. Execute the following commands as root on the salt-master node:
  - mkdir -p /srv/salt/distribution
  - cp /home/cloudbreak/cdp_telemetry-${EXPECTED_MIN_VERSION}.x86_64.rpm /srv/salt/distribution/
  - $SALT_BIN_PREFIX/salt '*' cp.get_file salt:///distribution/cdp_telemetry-${EXPECTED_MIN_VERSION}.x86_64.rpm /tmp/cdp_telemetry-${EXPECTED_MIN_VERSION}.x86_64.rpm
  - $SALT_BIN_PREFIX/salt '*' cmd.run 'yum remove -y cdp-telemetry && rpm -i /tmp/cdp_telemetry-${EXPECTED_MIN_VERSION}.x86_64.rpm'
  - $SALT_BIN_PREFIX/salt '*' cmd.run 'rm -r /tmp/cdp_telemetry-${EXPECTED_MIN_VERSION}.x86_64.rpm'
  - rm -rf /srv/salt/distribution/
EOF
    )
    do_exit 1 "$full_error_msg"
  else
    do_exit 0 "Diagnostics VM check finished successfully."
  fi
}

function run_operation() {
  while [[ $# -gt 0 ]]; do
      key="$1"
      case $key in
        -h|--hosts)
          HOSTS_FILTER="$2"
          log "Hosts filter: $HOSTS_FILTER"
          shift 2
        ;;
        *)
          echo "Unknown option: $1"
          do_exit 1
        ;;
      esac
  done
  if [[ "$WORKING_DIR" == "" ]]; then
    WORKING_DIR="/tmp"
  elif [[ ! -d "$WORKING_DIR" ]]; then
    log "Working directory does not exists. Creating it..."
    mkdir -p "$WORKING_DIR"
  fi
  if [[ "$OPERATION_NAME" == "master-check" ]]; then
    master_check
  fi
}

function main() {
  command="$1"
  case $command in
   "master-check")
      OPERATION_NAME="master-check"
      run_operation "${@:2}"
    ;;
   "help")
      print_help
    ;;
   *)
    echo "Available commands: (master-check | help)"
   ;;
   esac
}

init_salt_prefix
main ${1+"$@"}