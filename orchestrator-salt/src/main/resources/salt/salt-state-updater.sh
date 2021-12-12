#!/bin/sh

: ${LOGFILE_FOLDER:="/var/log/salt-state-updater"}
: ${WORKING_DIR:="/tmp/salt-state-updater"}
: ${SAlT_SRV_DIR:="/srv/salt"}


function print_help() {
  cat << EOF
   Extract compressed salt state definitions that will replace existing ones
   Usage: [<options>]
   options:
     -f, --file        zip file that contains salt state definitions.
     -s, --states      comma separated state definitions stat will be replaced.
     -h, --help        print usage.
EOF
}

function init_logfile() {
  mkdir -p $LOGFILE_FOLDER
  local timestamp=$(date +"%Y%m%d-%H%M%S")
  LOGFILE="$LOGFILE_FOLDER/salt-state-updater-${timestamp}.log"
  touch $LOGFILE
  chmod 600 $LOGFILE
  cleanup_old_logs
  log "The following log file will be used: $LOGFILE"
}

function cleanup_old_logs() {
  ls -1tr $LOGFILE_FOLDER/salt-state-updater*.log | head -n -5 | xargs --no-run-if-empty rm
}

function log() {
  local timestamp=$(date +"%Y-%m-%dT%H:%M:%S.%3N%z")
  local debug=$2
  echo "$timestamp $1" >> $LOGFILE
  if [[ "$debug" == "" ]]; then
    echo "$1"
  fi
}

function do_exit() {
  local code=$1
  local message=$2
  if [[ "$message" == "" ]]; then
    log "Exit code: $code"
  else
    log "Exit code: $code, $message"
  fi
  exit $code
}

function cleanup_workdir() {
  if [[ -d "$WORKING_DIR" ]]; then
    log "Cleanup workdir: $WORKING_DIR"
  fi
}

function setup_workdir() {
  cleanup_workdir
  mkdir -p "$WORKING_DIR/salt"
}

function process_states() {
  local zip_file=${1:?"usage: <zip_file>"}
  local states=${2:?"usage: <comma separated salt states>"}
  setup_workdir
  unzip -qo "${zip_file}" -d "$WORKING_DIR"
  local state_list_str=$(echo "$states" | tr ',' ' ')
  local state_arr=($(echo $state_list_str))
  for state in "${state_arr[@]}"
  do
    salt_component_state_dir="$SAlT_SRV_DIR/$state"
    salt_component_src_dir="$WORKING_DIR/salt/$state"
    if [[ -d "$salt_component_state_dir" && -d "$salt_component_src_dir" ]]; then
      log "Moving ${salt_component_state_dir} folder to ${salt_component_state_dir}.bkp"
      mv "${salt_component_state_dir}" "${salt_component_state_dir}.bkp"
      log "Copying $salt_component_src_dir into ${salt_component_state_dir}"
      cp -r "$salt_component_src_dir" "${salt_component_state_dir}"
      chmod 644 "${salt_component_state_dir}"
      log "Cleaning up backup: ${salt_component_state_dir}.bkp"
      rm -rf "${salt_component_state_dir}.bkp"
    elif [[ -d "$salt_component_src_dir" ]]; then
      log "Component folder does not exist in /srv/salt, but exists in the working directory, copying it ..."
      cp -r "$salt_component_src_dir" "${salt_component_state_dir}"
      chmod 644 "${salt_component_state_dir}"
    else
      log "Folder $salt_component_state_dir or $salt_component_src_dir do not exist. Skip processing ..."
      continue
    fi
  done
  cleanup_workdir
}

function main() {
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -f|--file)
          ZIP_FILE="$2"
          shift 2
        ;;
        -s|--states)
          SALT_STATES="$2"
          shift 2
        ;;
        -h|--help)
          print_help
        ;;
        *)
          echo "Unknown option: $1"
          do_exit 1
        ;;
      esac
    done
  init_logfile
  if [[ "$ZIP_FILE" == "" || "$SALT_STATES" == "" ]]; then
    do_exit 1 "Both --file,-f and --states,-s parameters are required."
  fi
  if [[ ! -f "$ZIP_FILE" ]]; then
    do_exit 1 "Compressed file $ZIP_FILE does not exist."
  fi
  process_states "$ZIP_FILE" "$SALT_STATES"
}

main ${1+"$@"}