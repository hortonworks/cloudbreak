#!/bin/sh

METERING_PACKAGE_NAME="thunderhead-metering-heartbeat-application"
METERING_FILE_NAME="$METERING_PACKAGE_NAME-0.1-SNAPSHOT.x86_64.rpm"
: ${METERING_ARCHIVE_URL:="https://archive.cloudera.com/cp_clients/$METERING_FILE_NAME"}
: ${METERING_SERVICE_DELIVERY_URL:="https://cloudera-service-delivery-cache.s3.amazonaws.com/$METERING_PACKAGE_NAME/clients/$METERING_FILE_NAME"}
: ${LOGFILE_FOLDER:="/var/log/metering-upgrade"}
CURL_CONNECT_TIMEOUT=60
MIN_BYTES=1000000

print_help() {
  cat << EOF
   Usage: [<command>] [<arguments with flags>]
   commands:
     upgrade              download and upgrade metering heartbeat agent
     help                 print usage
   upgrade command arguments:
     -d, --from-date      <yyyy-mm-dd>       Install RPM if the installation date is lower than this value.
     -u, --custom-rpm-url <CUSTOM RPM URL>   Custom URL that will be used instead of the default archive URL
EOF
}

do_exit() {
  local code=$1
  local message=$2
  if [[ "$message" == "" ]]; then
    info "Exit code: $code"
  else
    info "Exit code: $code, --- STATUS MESSAGE --- $message --- STATUS MESSAGE ---"
  fi
  exit $code
}

init_logfile() {
  mkdir -p $LOGFILE_FOLDER
  local timestamp=$(date +"%Y%m%d-%H%M%S")
  LOGFILE="$LOGFILE_FOLDER/metering-upgrade-${timestamp}.log"
  touch $LOGFILE
  cleanup_old_logs
  info "The following log file will be used: $LOGFILE"
}

cleanup_old_logs() {
  ls -1tr $LOGFILE_FOLDER/metering-upgrade*.log | head -n -3 | xargs --no-run-if-empty rm
}

info() {
  log "$1"
}

debug() {
  log "$1" "true"
}

log() {
  local timestamp=$(date +"%Y-%m-%dT%H:%M:%S.%3N%z")
  local debug=$2
  echo "$timestamp $1" >> $LOGFILE
  if [[ "$2" == "" ]]; then
    echo "$1"
  fi
}

rpm_upgrade() {
  local local_rpm_file=${1:?"usage: <local_rpm_file>"}
  debug "Execute command: yum remove -y $METERING_PACKAGE_NAME && rpm -i $local_rpm_file"
  yum remove -y $METERING_PACKAGE_NAME && rpm -i $local_rpm_file
  local upgrade_result="$?"
  if [[ "$upgrade_result" == 0 ]]; then
    do_exit 0 "UPGRADE FINISHED."
  else
    do_exit 1 "UPGRADE FAILED WITH CODE: $upgrade_result."
  fi
}

check_upgrade_by_date() {
  local from_date="$FROM_DATE"
  if [[ "$from_date" != "" ]]; then
    debug "Check that upgrade is required by date: $from_date"
    metering_package_date_unformatted=$(rpm -qa --last thunderhead-metering-heartbeat-application | cut -d " " -f 2-)
    metering_package_date=$(date -d "$metering_package_date_unformatted" '+%Y-%m-%d')
    if [[ "$from_date" < "$metering_package_date" ]]; then
      do_exit 0 "NO UPGRADE REQUIRED"
    fi
  fi
}

check_download_rpm_by_size() {
  local local_rpm_file=${1:?"usage: <local_rpm_file>"}
  if [[ ! -f $local_rpm_file ]]; then
    do_exit 1 "Cannot find file: $local_rpm_file"
  fi
  local rpm_file_size=$(du -sb $local_rpm_file | awk '{ print $1 }')
  local rs=$(expr $rpm_file_size + 0)
  local min=$(expr $MIN_BYTES + 0)
  if [ "$min" -gt "$rs" ]; then
    do_exit 1 "File $local_rpm_file is too small (bytes: $rs)"
  fi
}

download() {
  local local_rpm_file=${1:?"usage: <local_rpm_file>"}
  local archive_url="$METERING_ARCHIVE_URL"
  if [[ "$CUSTOM_RPM_URL" != "" ]]; then
    debug "Found custom RPM URL: $CUSTOM_RPM_URL"
    archive_url="$CUSTOM_RPM_URL"
  fi
  debug "Checking internet connection against url: $archive_url"
  curl ${CURL_EXTRA_PARAMS} --head -s -k $archive_url
  local archive_check_result="$?"
  if [[ "$archive_check_result" == "0" ]]; then
    download_binary "$archive_url" "$local_rpm_file" "true"
  else
    if [[ "$METERING_SERVICE_DELIVERY_URL" == "$METERING_ARCHIVE_URL" ]]; then
      do_exit 1 "Cannot download Metering RPM file (from $archive_url)."
    fi
    debug "Internet connection failed against $archive_url"
    debug "Checking internet connection against url: $METERING_SERVICE_DELIVERY_URL"
    curl ${CURL_EXTRA_PARAMS} --head -s -k $METERING_SERVICE_DELIVERY_URL
    local service_delivery_check_result="$?"
    if [[ "$service_delivery_check_result" == "0" ]]; then
      download_binary "$METERING_SERVICE_DELIVERY_URL" "$local_rpm_file" "true"
    else
      do_exit 1 "Cannot download Metering RPM file (from $archive_url or $METERING_SERVICE_DELIVERY_URL)."
    fi
  fi
}

download_binary() {
    local rpm_url=${1:?"usage: <rpm_url>"}
    local download_file_path=${2:?"usage: <download_file_path>"}
    local override=${3}
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

run_operation() {
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -d|--from-date)
          FROM_DATE="$2"
          shift 2
        ;;
        -u|--custom-rpm-url)
          CUSTOM_RPM_URL="$2"
          shift 2
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
  if [[ "$WORKING_DIR" == "" ]]; then
    WORKING_DIR="/tmp"
  elif [[ ! -d "$WORKING_DIR" ]]; then
    info "Working directory does not exists. Creating it..."
    mkdir -p "$WORKING_DIR"
    if [[ ! -d "$WORKING_DIR" ]]; then
      do_exit 1 "Workdir cannot be created: $WORKING_DIR"
    fi
  fi
  if [[ "$OPERATION_NAME" == "upgrade" ]]; then
    if [[ "$HTTPS_PROXY" != "" ]]; then
      export https_proxy="${HTTPS_PROXY}"
      info "Found https_proxy settings."
    fi
    if [[ "$NO_PROXY" != "" ]]; then
      export no_proxy="${NO_PROXY}"
      info "Found no_proxy settings: $no_proxy"
    fi
    local local_rpm_file="$WORKING_DIR/$METERING_FILE_NAME"
    check_upgrade_by_date
    download "$local_rpm_file"
    check_download_rpm_by_size "$local_rpm_file"
    rpm_upgrade "$local_rpm_file"
  fi
}

main() {
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