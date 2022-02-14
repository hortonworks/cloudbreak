#!/bin/bash

LOGFILE_FOLDER:="/var/log/cdp-telemetry-deployer"

function print_help() {
  cat << EOF
   Usage: [<command>] [<arguments with flags>]
   commands:
     upgrade            upgrade or install cdp infra tools (cdp-telemetry, cdp-logging-agent)
     help               print usage
   upgrade command arguments:
     -c, --component  <COMPONENT NAME>       Name of the component, values: cdp-telemetry | cdp-logging-agent
EOF
}

function do_exit() {
  local code=$1
  local message=$2
  if [[ "$message" == "" ]]; then
    info "Exit code: $code"
  else
    info "Exit code: $code, --- STATUS MESSAGE --- $message --- STATUS MESSAGE ---"
  fi
  exit $code
}

function init_logfile() {
  mkdir -p $LOGFILE_FOLDER
  local timestamp=$(date +"%Y%m%d-%H%M%S")
  LOGFILE="$LOGFILE_FOLDER/cdp-telemetry-deployer-${timestamp}.log"
  touch $LOGFILE
  cleanup_old_logs
  info "The following log file will be used: $LOGFILE"
}

function cleanup_old_logs() {
  ls -1tr $LOGFILE_FOLDER/cdp-telemetry-deployer*.log | head -n -3 | xargs --no-run-if-empty rm
}

function info() {
  log "$1"
}

function debug() {
  log "$1" "true"
}

function log() {
  local timestamp=$(date +"%Y-%m-%dT%H:%M:%S.%3N%z")
  local debug=$2
  echo "$timestamp $1" >> $LOGFILE
  if [[ "$2" == "" ]]; then
    echo "$1"
  fi
}

function is_component_installed() {
  local component=${1:?"usage: <component>"}
  local installed=$(rpm -q "$component" 2>&1 >/dev/null; echo $?)
  echo "$installed"
}

function check_local_version() {
    local component=${1:?"usage: <component>"}
    local installed=$(is_component_installed "$component")
    if [[ "$installed" == "1" ]]; then
        echo "-1"
    elif [[ "$component" == "cdp-logging-agent" || "$component" == "cdp-telemetry" ]]; then
        local telemetry_tool_version=$(rpm -q --queryformat '%-{VERSION}' "$component")
        echo "$telemetry_tool_version"
    else
        echo "-1"
    fi
}

function cleanup_td_agent() {
  local component=${1:?"usage: <component>"}
  if [[ "$component" == "cdp-logging-agent" ]]; then
    log "Checking deprecated td-agent installation."
    local td_agent_res=$(is_component_installed "td-agent")
    if [[ "$td_agent_res" == "0" ]]; then
      log "Found td-agent, stopping and removing it... Running command: rpm -e --nodeps td-agent"
      rpm -e --nodeps td-agent
    else
      log "Not found any previous td-agent installation."
    fi
  fi
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

function upgrade_package() {
  local component=${1:?"usage: <component>"}
  local expected_version=${2}
  local local_version=$(check_local_version "$component")
  if [[ "$local_version" == "-1" ]]; then
    cleanup_td_agent "$component"
    yum clean all --disablerepo="*" --enablerepo=cdp-infra-tools
    yum install -y "${component}"
  else
    local version_cmp_result=$(version_lt $expected_version $local_version)
    if [[ "$version_cmp_result" == "0" ]]; then
      do_exit 0 "UPGRADE IS NOT REQUIRED"
    else
      yum clean all --disablerepo="*" --enablerepo=cdp-infra-tools
      yum remove -y "${component}"
      yum install -y "${component}"
    fi
  fi
}

function run_operation() {
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -c|--component)
          COMPONENT="$2"
          shift 2
        ;;
        -v|--version)
          VERSION="$2"
          shift 2
        ;;
        *)
          echo "Unknown option: $1"
          do_exit 1
        ;;
      esac
    done
    init_logfile
    if [[ "$COMPONENT" == "" ]]; then
      do_exit 1 "Component option is missing. It needs to be set to cdp-logging-agent or cdp-telemetry"
    elif [[ "$COMPONENT" != "cdp-telemetry" && "$COMPONENT" != "cdp-logging-agent" ]]; then
      do_exit 1 "Component option is invalid. (value: $COMPONENT) It needs to be set to cdp-logging-agent or cdp-telemetry)"
    fi
    if [[ "$https_proxy" != "" ]]; then
      export https_proxy="${https_proxy}"
      log "Found https_proxy settings."
    fi
    if [[ "$no_proxy" != "" ]]; then
      export no_proxy="${no_proxy}"
      log "Found no_proxy settings: $no_proxy"
    fi
    if [[ "$OPERATION_NAME" == "upgrade" ]]; then
      upgrade_package "${COMPONENT}" "${VERSION}"
    fi
}

function main() {
  command="$1"
  case $command in
   "upgrade")
      OPERATION_NAME="upgrade"
      run_operation "${@:2}"
    ;;
   "help")
      print_help
    ;;
   *)
    echo "Available commands: (upgrade | help)"
   ;;
   esac
}

main ${1+"$@"}