#!/bin/sh

: ${CDP_TELEMETRY_BASE_URL:="https://cloudera-service-delivery-cache.s3.amazonaws.com/telemetry/cdp-telemetry/"}
: ${CDP_LOGGING_AGENT_BASE_URL:="https://cloudera-service-delivery-cache.s3.amazonaws.com/telemetry/cdp-logging-agent/"}
: ${LOGFILE_FOLDER:="/var/log/cdp-telemetry-deployer"}

AVAILABLE_CDP_TELEMETRY_VERSIONS_URL="${CDP_TELEMETRY_BASE_URL}AVAILABLE_VERSIONS"
AVAILABLE_CDP_LOGGING_AGENT_VERSIONS_URL="${CDP_LOGGING_AGENT_BASE_URL}AVAILABLE_VERSIONS"
MINION_SCRIPTS_FOLDER="/opt/salt/scripts"
CURL_CONNECT_TIMEOUT=60
SALT_CMD_TIMEOUT=120

# todo use minion script

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
     install            install cdp-telemetry tools locally (cdp-telemetry, cdp-logging-agent)
     upgrade            upgrade cdp-telemetry tools (cdp-telemetry, cdp-logging-agent) by salt
     download           download (only) cdp-telemetry tools locally (cdp-telemetry, cdp-logging-agent)
     help               print usage
   upgrade command arguments:
     -c, --component  <COMPONENT NAME>       Name of the component, values: cdp-telemetry | cdp-logging-agent
     -f, --rpm-file   <LOCAL RPM FILE>       Local RPM file to be installed (install or upgrade)
     -v, --version                           Picked cdp telemetry tool version (if exists) - needs to be a valid version
     -d, --downgrade                         Let the package to be downgraded if the already existing package version is higher
     -s, --skip-restart                      Skip restarting components during upgrade
     -w, --working-dir <DIRECTORY>           Location to store temporal files for updating binaries. (default: /tmp)
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

function init_salt_prefix() {
  SALT_BIN_PREFIX=$(find /opt -maxdepth 1 -type d -iname "salt_*" | xargs -I{} echo "{}/bin")
  if [[ "$SALT_BIN_PREFIX" == "" ]]; then
    SALT_BIN_PREFIX="/opt/salt_*/bin"
  fi
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

function run_command() {
  local cmd=${1:?"usage: <command>"}
  debug "The following command will be executed: $1"
  eval $1 >> $LOGFILE 2>&1
}

function list_available_versions() {
    local AVAILABLE_BASE_URL=${1:?"usage: <base_url>"}
    avail_versions=$(curl -L -k -s "${AVAILABLE_BASE_URL}" | tr "\n" " ")
    avail_versions_arr=($avail_versions)
    for version in "${avail_versions_arr[@]}"
    do
        echo "$version"
    done
}

function get_available_version() {
    local AVAILABLE_BASE_URL=${1:?"usage: <base_url>"}
    local PICKED_VERSION=$2
    avail_versions=$(curl -L -k -s $CURL_EXTRA_PARAMS "${AVAILABLE_BASE_URL}" | tr "\n" " ")
    avail_versions_arr=($avail_versions)
    if [[ "${PICKED_VERSION}" == "snapshot" ]]; then
        echo "0.1.0-SNAPSHOT"
    elif [[ "${PICKED_VERSION}" != "" ]]; then
      local found="false"
      for i in "${avail_versions_arr[@]}"
      do
        if [ "$i" == "$PICKED_VERSION" ] ; then
          found="true"
        fi
      done
      if [[ "$found" == "true" ]]; then
        echo "$PICKED_VERSION"
      else
        echo "-1"
      fi
    else
        echo "${avail_versions_arr[0]}"
    fi
}

function check_download() {
    local available_versions_url=${1:?"usage: <available url>"}
    info "Checking internet connection against telemetry repo url: $available_versions_url"
    curl ${CURL_EXTRA_PARAMS} --head -s -k ${available_versions_url}
    local result="$?"
    info "Response code: $result"
    if [[ "$result" == "0" ]]; then
        log "Download repo can be reached from the node. (tested url: ${available_versions_url})"
    else
        do_exit 1 "Cannot reach $available_versions_url with the current network settings."
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

function get_rpm_file_version() {
  local rpm_file=${1:?"usage: <rpm_file>"}
  local rpm_file_version=$(rpm -qp --queryformat '%-{VERSION}' $rpm_file)
  echo "$rpm_file_version"
}

function download_binary() {
    local rpm_url=${1:?"usage: <rpm_url>"}
    local rpm_name=${2:?"usage: <rpm_name>"}
    local version_to_download=${3:?"usage <version to download>"}
    local override=${4}
    local download_file_path="$WORKING_DIR/${rpm_name}-${version_to_download}.x86_64.rpm"
    if [[ -f $download_file_path && "$override" == "" ]]; then
      log "RPM file '$download_file_path' already exists. Download will be skipped."
      return
    fi
    log "Downloading $rpm_url"
    content_length=$(curl ${CURL_EXTRA_PARAMS} -s --head -k -L "${rpm_url}" | grep -i Content-Length | awk '{print $2}' | tr -d '\r' | tr -d '\n')
    log "Content length: ${content_length}"
    free_space="$(df -B1 --output=avail "$WORKING_DIR" | grep -v Avail)"
    log "Free space: ${free_space}"
    local fs=$(expr $free_space + 0)
    local cl=$(expr $content_length + 0)
    if [ "$fs" -gt "$cl" ]; then
      log "Have enough space for download the rpm from: $rpm_url"
    else
      do_exit 1 "Not enough space to download the rpm from: $rpm_url)"
    fi
    log "Run command: curl -k -L --output $download_file_path $rpm_url"
    curl ${CURL_EXTRA_PARAMS} -k -L --output "$download_file_path" "$rpm_url"
    if [[ ! -f $download_file_path ]]; then
      do_exit 1 "RPM file was not downloaded: $download_file_path. Exiting ..."
    fi
}

function list_minions_by_state_in_files() {
    local component=${1:?"usage: <component>"}
    local expected_version=${2:?"usage: <version>"}
    rm -rf $WORKING_DIR/${component}_to_install.txt && touch $WORKING_DIR/${component}_to_install.txt
    rm -rf $WORKING_DIR/${component}_to_upgrade.txt && touch $WORKING_DIR/${component}_to_upgrade.txt
    rm -rf $WORKING_DIR/${component}_do_nothing.txt && touch $WORKING_DIR/${component}_do_nothing.txt
    rm -rf $WORKING_DIR/${component}_not_responding.txt && touch $WORKING_DIR/${component}_not_responding.txt
    log "Gather pkg versions for $component"
    run_command "$SALT_BIN_PREFIX/salt '*' pkg.version $component --out-indent=-1 --out=json --out-file=$WORKING_DIR/${component}_versions.json --timeout=60 timeout=$SALT_CMD_TIMEOUT"
    for row in $(cat "$WORKING_DIR/${component}_versions.json" | jq -r 'to_entries | . []|=.key+":"+.value | @base64'); do
        local decoded_row=$(echo "${row}" | base64 -d | jq .[] | tr -d '"')
        local salt_minion_node=$(echo "${decoded_row}" | cut -d ':' -f1)
        local full_value=$(echo "${decoded_row}" | cut -d ':' -f2)
        local binary_version=$(echo "${full_value}" | cut -d '-' -f1)
        log "Found node/$component version pair: ${salt_minion_node} - ${binary_version}"
        if [[ "${full_value}" == *"Minion did not return"* ]];then
          log "Salt minion with name '${salt_minion_node}' is not responding."
          echo "${salt_minion_node}" >> $WORKING_DIR/${component}_not_responding.txt
        elif [[ "${binary_version}" == "${expected_version}" ]]; then
          log "Salt minion with name '${salt_minion_node}' is up to date (version: ${expected_version})."
          echo "${salt_minion_node}" >> $WORKING_DIR/${component}_do_nothing.txt
        elif [[ "${binary_version}" != "" ]]; then
          if [[ "$DOWNGRADE" == "true" ]]; then
            log "Salt minion with name '${salt_minion_node}' needs an upgrade for ${component} (version: ${expected_version})."
            echo "${salt_minion_node}" >> $WORKING_DIR/${component}_to_upgrade.txt
          else
            local lower_val_in_binary=$(version_lt $expected_version $binary_version )
            if [[ "$lower_val_in_binary" == "0" ]]; then
              log "Salt minion with name '${salt_minion_node}' cannot be downgraded to version ${expected_version} (from ${binary_version}) for ${component}"
            else
              log "Salt minion with name '${salt_minion_node}' needs an upgrade for ${component} (version: ${expected_version})."
              echo "${salt_minion_node}" >> $WORKING_DIR/${component}_to_upgrade.txt
            fi
          fi
        else
          log "Salt minion with name '${salt_minion_node}' needs a $component installation."
          echo "${salt_minion_node}" >> $WORKING_DIR/${component}_to_install.txt
        fi
    done
    if [[ -s $WORKING_DIR/${component}_not_responding.txt ]]; then
      local not_responding_hosts=$(cat $WORKING_DIR/${component}_not_responding.txt | tr '\n' ',' | sed 's/.$//')
      log "Warning: the following hosts are not responding: $not_responding_hosts"
    fi
    if [[ ! -s $WORKING_DIR/${component}_to_install.txt && ! -s $WORKING_DIR/${component}_to_upgrade.txt ]]; then
      log "Both upgrade files ($WORKING_DIR/${component}_to_install.txt and $WORKING_DIR/${component}_to_upgrade.txt) are empty."
      do_exit 0 "No need for performing upgrade on minions."
    fi
}

function start_component_services() {
  local component=${1:?"usage: <component>"}
  local targets=$2
  if [[ "$component" == "cdp-logging-agent" ]]; then
    run_command "$SALT_BIN_PREFIX/salt -L $targets state.apply fluent.init timeout=$SALT_CMD_TIMEOUT"
  elif [[ "$component" == "cdp-telemetry" ]]; then
    run_command "$SALT_BIN_PREFIX/salt -C 'G@roles:manager_server or G@roles:freeipa_primary or G@roles:freeipa_replica or G@roles:freeipa_primary_replacement' cmd.run 'systemctl is-enabled --quiet cdp-nodestatus-monitor && systemctl daemon-reload && systemctl start --quiet cdp-nodestatus-monitor' timeout=$SALT_CMD_TIMEOUT"
    run_command "$SALT_BIN_PREFIX/salt -C 'G@roles:manager_server' cmd.run 'systemctl is-enabled --quiet cdp-metrics-collector && systemctl daemon-reload && systemctl start --quiet cdp-metrics-collector' timeout=$SALT_CMD_TIMEOUT"
  fi
}

function stop_component_services() {
  local component=${1:?"usage: <component>"}
  local targets=$2
  if [[ "$component" == "cdp-logging-agent" ]]; then
    run_command "$SALT_BIN_PREFIX/salt -L $targets state.apply fluent.agent-stop timeout=$SALT_CMD_TIMEOUT"
  elif [[ "$component" == "cdp-telemetry" ]]; then
    run_command "$SALT_BIN_PREFIX/salt -C 'G@roles:manager_server and G@freeipa_primary and G@freeipa_replica and G@freeipa_primary_replacement' cmd.run 'systemctl is-active --quiet cdp-nodestatus-monitor && systemctl stop --quiet cdp-nodestatus-monitor' timeout=$SALT_CMD_TIMEOUT"
    run_command "$SALT_BIN_PREFIX/salt -C 'G@roles:manager_server' cmd.run 'systemctl is-active --quiet cdp-metrics-collector && systemctl stop --quiet cdp-metrics-collector' timeout=$SALT_CMD_TIMEOUT"
  fi
}

function run_salt_dist_and_install() {
  local rpm_file_name=${1:?"usage: <rpm file name>"}
  local target_type=${2:?"usage: <target type>"}
  local component=${3:?"usage: <component>"}
  local targets=$4
  if [[ "$targets" != "" ]]; then
    if [[ "$SKIP_RESTART" != "true" ]]; then
      stop_component_services "$component" "$targets"
    fi
    log "Targets are not empty for target type '$target_type'"
    run_command "$SALT_BIN_PREFIX/salt -L $targets cmd.run 'rm -rf /tmp/cdp_*.rpm' timeout=$SALT_CMD_TIMEOUT"
    run_command "$SALT_BIN_PREFIX/salt -L $targets cp.get_file salt:///distribution/cdp_telemetry_deployer.sh $MINION_SCRIPTS_FOLDER/cdp_telemetry_deployer.sh"
    run_command "$SALT_BIN_PREFIX/salt -L $targets cp.get_file salt:///distribution/$rpm_file_name /tmp/$rpm_file_name"
    run_command "$SALT_BIN_PREFIX/salt -L $targets cmd.run 'chmod 750 $MINION_SCRIPTS_FOLDER/cdp_telemetry_deployer.sh && $MINION_SCRIPTS_FOLDER/cdp_telemetry_deployer.sh install -c $component -f /tmp/$rpm_file_name' timeout=$SALT_CMD_TIMEOUT"
    run_command "$SALT_BIN_PREFIX/salt -L $targets cmd.run 'rm -rf /tmp/cdp_*.rpm' timeout=$SALT_CMD_TIMEOUT"
    if [[ "$SKIP_RESTART" != "true" ]]; then
      start_component_services "$component" "$targets"
    fi
  else
    log "Targets are empty for target type '$target_type'. Skip running any salt operations on them."
  fi
}

function distribute_files_and_install() {
  local component=${1:?"usage: <component>"}
  local rpm_file_name=${2:?"usage: <rpm file name>"}
  local distribution_folder="/srv/salt/distribution"
  local rpm_location="$WORKING_DIR/$rpm_file_name"

  local install_targets=$(cat "$WORKING_DIR/${component}_to_install.txt" | paste -sd "," -)
  local upgrade_targets=$(cat "$WORKING_DIR/${component}_to_upgrade.txt" | paste -sd "," -)

  log "Creating $distribution_folder if does not exists."
  mkdir -p $distribution_folder
  log "Copying $SCRIPT_LOCATION into $distribution_folder."
  cp -r $SCRIPT_LOCATION $distribution_folder/
  log "Moving $rpm_location into $distribution_folder."
  mv $rpm_location $distribution_folder/

  run_salt_dist_and_install "$rpm_file_name" "install" "$component" "$install_targets"
  run_salt_dist_and_install "$rpm_file_name" "upgrade" "$component" "$upgrade_targets"
  rm -rf $distribution_folder/cdp*.rpm
  rm -rf $distribution_folder/cdp*.sh
}

function handle_error_response() {
  if [[ "$1" == "-1" ]]; then
    if [[ "$2" != "" ]]; then
      log "$2. Exiting ..."
    else
      log "Stop processing as response code is -1. Exiting ..."
    fi
    do_exit 1 $2
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
      log "Not found any prvious td-agent installation."
    fi
  fi
}

function cleanup_cdp_telemetry() {
  local component=${1:?"usage: <component>"}
  if [[ "$component" == "cdp-telemetry" ]]; then
    yum remove -y cdp-telemetry
  fi
}

function init_proxy_configs() {
  local proxy_settings=$($SALT_BIN_PREFIX/salt-call pillar.items --out=json | jq .local.proxy)
  CURL_EXTRA_PARAMS="--connect-timeout ${CURL_CONNECT_TIMEOUT}"
  if [[ "${proxy_settings}" != "null" ]]; then
    local proxy_protocol=$(echo "${proxy_settings}" | jq .protocol | tr -d '"')
    local proxy_host=$(echo "${proxy_settings}" | jq .host | tr -d '"')
    local proxy_user=$(echo "${proxy_settings}" | jq .user | tr -d '"')
    local proxy_password=$(echo "${proxy_settings}" | jq .password | tr -d '"')
    local no_proxy_hosts=$(echo "${proxy_settings}" | jq .noProxyHosts | tr -d '"')
    if [[ "${proxy_host}" != "null" ]]; then
      local proxy_url="${proxy_protocol}://${proxy_host}"
      info "Https proxy is set: ${proxy_url}"
      CURL_EXTRA_PARAMS="${CURL_EXTRA_PARAMS} --proxy ${proxy_url}"
      if [[ "${proxy_user}" != "null" ]]; then
        https_proxy="${proxy_protocol}://${proxy_user}:${proxy_password}@${proxy_host}"
        CURL_EXTRA_PARAMS="${CURL_EXTRA_PARAMS} --proxy-user ${proxy_user}:${proxy_password}"
      else
        https_proxy="${proxy_url}"
      fi
    fi
    if [[ "${no_proxy_hosts}" != "null" ]]; then
      no_proxy=${no_proxy_hosts}
      CURL_EXTRA_PARAMS="${CURL_EXTRA_PARAMS} --noproxy ${no_proxy}"
    fi
  else
    info "Https proxy is not set."
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

function install_local() {
  local component=${1:?"usage: <component>"}
  local rpm_file=${2:?"usage: <rpm file>"}
  local local_version=$(check_local_version "$component")
  if [[ ! -f $rpm_file ]]; then
    do_exit 1 "RPM file '$rpm_file' does not exist. Stop installation."
  fi
  local rpm_version=$(get_rpm_file_version "$rpm_file")
  log "Parameters for install - component: $component, rpm file: $rpm_file, rpm version: $rpm_version, local version: $local_version"
  if [[ "$local_version" == "-1" ]]; then
    cleanup_td_agent "$component"
    log "Component $component is not installed. Installing ..."
    log "Execute command: rpm -i $rpm_file"
    rpm -i $rpm_file
  elif [[ "$rpm_version" == "$local_version" ]]; then
    log "Component $component is up to date, do nothing"
  else
    local lower_val=$(version_lt $rpm_version $local_version)
    if [[ "$lower_val" == "0" ]]; then
      cleanup_cdp_telemetry "$component"
      log "RPM version is lower than local version. Downgrading $component..."
      log "Executing command: rpm -U --oldpackage $rpm_file"
      rpm -U --oldpackage $rpm_file
    elif [[ "$lower_val" == "1" ]]; then
      cleanup_cdp_telemetry "$component"
      log "Local binary version is lower than installable RPM version. Upgrading $component..."
      log "Executing command: rpm -U $rpm_file"
      rpm -U $rpm_file
      #set_component_grain_version "$component" "$rpm_version"
    fi
  fi
}

function install_package_component_from_local_rpm() {
  local component=${1:?"usage: <component>"}
  local rpm_file=${2:?"usage: <rpm file>"}
  local rpm_version=$(get_rpm_file_version "$rpm_file")
  if [[ "$component" == "cdp-logging-agent" ]]; then
    local rpm_name="cdp_logging_agent"
  elif [[ "$component" == "cdp-telemetry" ]]; then
    local rpm_name="cdp_telemetry"
  else
    do_exit 1 "Component '$component' does not exist."
  fi
  local rpm_location="$WORKING_DIR/${rpm_name}-${rpm_version}.x86_64.rpm"
  if [[ "$rpm_file" != "$rpm_location" ]]; then
    cp -r $rpm_file $rpm_location
  fi
  list_minions_by_state_in_files "$component" "$rpm_version"
  distribute_files_and_install "$component" "${rpm_name}-${rpm_version}.x86_64.rpm"
}

# todo: set latest roles during upgrade
#function set_component_grain_version() {
#  local component=${1:?"usage: <component>"}
#  local package_version=${2:?"usage: <package_version>"}
#  local resolved_grain_name=$(echo "${component}_version"| tr '-' '_')
#  run_command "$SALT_BIN_PATH/salt-call --local grains.setval $resolved_grain_name $package_version"
#}

function install_package_component() {
  local component=${1:?"usage: <component>"}
  local distributed=${2:?"usage: <distributed flag>"}
  local download_only=${3}
  if [[ "$component" == "cdp-logging-agent" ]]; then
    local local_version=$(check_local_version "$component")
    check_download "$AVAILABLE_CDP_LOGGING_AGENT_VERSIONS_URL"
    local version_to_download=$(get_available_version "${AVAILABLE_CDP_LOGGING_AGENT_VERSIONS_URL}" "$CDP_TOOL_VERSION")
    handle_error_response $version_to_download "Picked version is invalid"
    local rpm_url="${CDP_LOGGING_AGENT_BASE_URL}${version_to_download}/cdp_logging_agent-${version_to_download}.x86_64.rpm"
    local rpm_name="cdp_logging_agent"
  elif [[ "$component" == "cdp-telemetry" ]]; then
    local local_version=$(check_local_version "$component")
    check_download "$AVAILABLE_CDP_TELEMETRY_VERSIONS_URL"
    local version_to_download=$(get_available_version "${AVAILABLE_CDP_TELEMETRY_VERSIONS_URL}" "$CDP_TOOL_VERSION")
    handle_error_response $version_to_download "Picked version is invalid"
    local rpm_url="${CDP_TELEMETRY_BASE_URL}cdp_telemetry-${version_to_download}.x86_64.rpm"
    local rpm_name="cdp_telemetry"
  else
    do_exit 1 "Component '$component' does not exist."
  fi
  if [[ "$distributed" == "false" && "$local_version" == "$version_to_download" ]]; then
    do_exit 0 "Current version is matching with the installable package. Skipping upgrade..."
  fi
  if [[ "$distributed" == "true" ]]; then
    list_minions_by_state_in_files "$component" "$version_to_download"
  fi
  download_binary "$rpm_url" "$rpm_name" "$version_to_download" "$download_only"
  if [[ "$download_only" == "true" ]]; then
    do_exit 0 "DOWNLOAD FINISHED"
  fi
  if [[ "$distributed" == "true" ]]; then
    distribute_files_and_install "$component" "$rpm_name-${version_to_download}.x86_64.rpm"
    do_exit 0 "DISTRIBUTED UPGRADE FINISHED"
  else
    install_local "$component" "$WORKING_DIR/${rpm_name}-${version_to_download}.x86_64.rpm"
    do_exit 0
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
        -f|--rpm-file)
          LOCAL_RPM_FILE="$2"
          shift 2
        ;;
        -v|--version)
          CDP_TOOL_VERSION="$2"
          shift 2
        ;;
        -d|--downgrade)
          DOWNGRADE="true"
          shift 1
        ;;
        -s|--skip-restart)
          SKIP_RESTART="true"
          shift 1
        ;;
        -w|--working-dir)
          WORKING_DIR="$2"
          shift 2
        ;;
        *)
          echo "Unknown option: $1"
          do_exit 1
        ;;
      esac
    done
    init_logfile
    init_salt_prefix
    if [[ "$WORKING_DIR" == "" ]]; then
      WORKING_DIR="/tmp"
    elif [[ ! -d "$WORKING_DIR" ]]; then
      log "Working directory does not exists. Creating it..."
      mkdir -p "$WORKING_DIR"
    fi
    if [[ "$CDP_TOOL_VERSION" == "snapshot" ]]; then
      log "Downgrade is set as snapshot version is used."
      DOWNGRADE="true"
    fi
    if [[ "$COMPONENT" == "" ]]; then
      do_exit 1 "Component option is missing. It needs to be set to cdp-logging-agent or cdp-telemetry"
    elif [[ "$COMPONENT" != "cdp-telemetry" && "$COMPONENT" != "cdp-logging-agent" ]]; then
      do_exit 1 "Component option is invalid. (value: $COMPONENT) It needs to be set to cdp-logging-agent or cdp-telemetry)"
    fi
    init_proxy_configs
    if [[ "$https_proxy" != "" ]]; then
      export https_proxy="${https_proxy}"
      log "Found https_proxy settings."
    fi
    if [[ "$no_proxy" != "" ]]; then
      export no_proxy="${no_proxy}"
      log "Found no_proxy settings: $no_proxy"
    fi
    if [[ "$OPERATION_NAME" == "install" ]]; then
      if [[ "$LOCAL_RPM_FILE" == "" ]]; then
        log "RPM file location is missing. Install package from AVAILABLE_VERSIONS"
        install_package_component "$COMPONENT" "false"
      else
        if [[ ! -f "$LOCAL_RPM_FILE" ]]; then
          do_exit 1 "RPM file '$LOCAL_RPM_FILE' does not exist. Exiting ..."
        fi
        install_local "$COMPONENT" "$LOCAL_RPM_FILE"
      fi
    elif [[ "$OPERATION_NAME" == "upgrade" ]]; then
      if [[ "$LOCAL_RPM_FILE" == "" ]]; then
        install_package_component "$COMPONENT" "true"
      else
        if [[ ! -f "$LOCAL_RPM_FILE" ]]; then
          do_exit 1 "RPM file '$LOCAL_RPM_FILE' does not exist. Exiting ..."
        fi
        install_package_component_from_local_rpm "$COMPONENT" "$LOCAL_RPM_FILE"
      fi
    elif [[ "$OPERATION_NAME" == "download" ]]; then
      install_package_component "$COMPONENT" "true" "true"
    fi
}

function main() {
  command="$1"
  case $command in
   "install")
      OPERATION_NAME="install"
      run_operation "${@:2}"
    ;;
   "upgrade")
      OPERATION_NAME="upgrade"
      run_operation "${@:2}"
    ;;
   "download")
      OPERATION_NAME="download"
      run_operation "${@:2}"
    ;;
   "help")
      print_help
    ;;
   *)
    echo "Available commands: (install | upgrade | download | help)"
   ;;
   esac
}

main ${1+"$@"}