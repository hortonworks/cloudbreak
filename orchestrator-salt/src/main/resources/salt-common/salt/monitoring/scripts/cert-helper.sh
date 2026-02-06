#!/bin/bash

VALIDITY_IN_DAYS=365
REGENERATE_IN_DAY_RANGE=7
EXPIRY_RANGE_IN_SECONDS=$(( 86400*$REGENERATE_IN_DAY_RANGE))
LOGFILE_FOLDER="/var/log/cdp-monitoring-cert-helper"

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
  mkdir -p $LOGFILE_FOLDER
  local timestamp=$(date +"%Y%m%d-%H%M%S")
  local log_file_name="cdp-monitoring-cert-helper"
  LOGFILE="$LOGFILE_FOLDER/$log_file_name-${timestamp}.log"
  touch $LOGFILE
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
  if [[ "$2" == "" ]]; then
    echo "$1"
  fi
}

function generate_cert_and_key() {
  local key_file=$1
  local cert_file=$2
  local CN_ATTR=$3
  openssl req -x509 -newkey rsa:3072 -keyout ${key_file} -out ${cert_file} -days $VALIDITY_IN_DAYS -nodes -config <(
cat <<-EOF
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no
[req_distinguished_name]
C = $C_ATTR
CN = $CN_ATTR
[v3_req]
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
DNS.2 = $ALT_DOMAIN
EOF
)

  if [[ "$?" != "0" ]]; then
    do_exit 1 "Cert/file generation failed."
  fi
  chmod 600 $key_file
  chmod 600 $cert_file
  restart_services
}

function restart_services() {
  if [[ "$SERVICES_TO_RESTART" != "" ]]; then
    IFS="," read -a services_to_restart <<< $SERVICES_TO_RESTART
    for service_to_restart in "${services_to_restart[@]}"
    do
      log "Restart service: $service_to_restart"
      systemctl restart $service_to_restart
    done
  else
    log "No need for restarting any services."
  fi
}

function generate_certs() {
  IFS="," read -a bases <<< $BASE_NAMES
  for base in "${bases[@]}"
  do
    log "Base name for cert/key: $base"
    base_dir=`dirname $base`
    log "Check base directory: $base_dir"
    if [[ ! -d "$base_dir" ]]; then
      do_exit 1 "Base dir '$base_dir' does not exists"
    fi
    cert_file="${base}.crt"
    key_file="${base}.key"
    if [[ -f "${cert_file}" && -f "${key_file}" ]]; then
      log "Files exists: ${cert_file} and ${key_file}"
      expiry_date=$(openssl x509 -enddate -noout -in ${cert_file})
      log "Expiration: $expiry_date"
      log "Execute: openssl x509 -checkend $EXPIRY_RANGE_IN_SECONDS -noout -in ${cert_file}"
      openssl x509 -checkend $EXPIRY_RANGE_IN_SECONDS -noout -in ${cert_file}
      if [[ "$?" == "0" ]]; then
        log "Cert/key won't be re-generated."
      else
        log "Cert/key needs to be re-generated ..."
        generate_cert_and_key "${key_file}" "${cert_file}" "$(basename $base)"
      fi
    else
      log "Files does not exist: ${cert_file} and/or ${key_file} - generating new cert/key ..."
      generate_cert_and_key "${key_file}" "${cert_file}" "$(basename $base)"
    fi
  done
  do_exit 0 "Cert/key generation finished."
}

function main() {
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -b|--base-names)
          BASE_NAMES="$2"
          shift 2
        ;;
        -c|--country-name)
          C_ATTR="$2"
          shift 2
        ;;
        -s|--services-restart-on-regeneration)
          SERVICES_TO_RESTART="$2"
          shift 2
        ;;
        *)
          echo "Unknown option: $1"
          exit 1
        ;;
      esac
    done
  if [[ "$BASE_NAMES" == "" ]];then
    echo "Requires: -b / --base-names flag (format: <folder>/<cert/key name>)"
    exit 1
  fi
  if [[ "$C_ATTR" == "" ]]; then
    C_ATTR="us"
  fi
  ALT_DOMAIN=$(hostname -f)
  init_logfile
  generate_certs
}

main ${1+"$@"}