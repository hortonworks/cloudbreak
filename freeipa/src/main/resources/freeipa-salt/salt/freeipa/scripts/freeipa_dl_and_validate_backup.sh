#!/bin/bash
# Name: freeipa_dl_and_validate_backup.sh
# Description: Download FreeIPA backup from provided Cloud Location and validate
################################################################
set -x

CONFIG_FILE=/etc/freeipa_backup.conf

LOCKFILE="/var/lock/$(basename "$0")"
LOCKFD=100
PRINT_DEBUG_MSGS=1
FORCE_LOG="true"

: "${FULL_BACKUP_LOCATION:? required}"
: "${DATA_BACKUP_LOCATION:? required}"

# PRIVATE
_lock()             { flock -"$1" ${LOCKFD}; }
_no_more_locking()  { _lock u; _lock xn && rm -f "${LOCKFILE}"; }
_prepare_locking()  { eval "exec ${LOCKFD}>\"${LOCKFILE}\""; trap _no_more_locking EXIT; }

# PUBLIC
exlock_now()        { _lock xn; }  # obtain an exclusive lock immediately or fail
exlock()            { _lock x; }   # obtain an exclusive lock
shlock()            { _lock s; }   # obtain a shared lock
unlock()            { _lock u; }   # drop a lock


# Config Defaults
typeset -A config # init array
config=( # set default values in config array
    [backup_location]=""
    [backup_platform]="LOCAL"
    [azure_instance_msi]=""
    [gcp_service_account]=""
    [logfile]="/var/log/iparestore.log"
    [statusfileprefix]="/var/log/iparestore_status_"
    [backup_path]="/var/lib/ipa/restore"
    [http_proxy]=""
    [aws_region]=""
    [aws_endpoint]=""
    [full_dir]="full"
    [data_dir]="data"
)

set +x
# Override defaults with config file
if [[ -f $CONFIG_FILE ]]; then
    while read -r line
    do
        if echo "${line}" | grep -F = &>/dev/null
        then
            varname=$(echo "${line}" | cut -d '=' -f 1)
            config[${varname}]=$(echo "${line}" | cut -d '=' -f 2-)
        fi
    done < $CONFIG_FILE
fi
set -x


LOGFILE="${config[logfile]}"
STATUSFILEPREFIX="${config[statusfileprefix]}"
BACKUP_PATH_POSTFIX="${FOLDER}"


doLog(){
    type_of_msg=$(echo "$*"|cut -d" " -f1)
    msg=$(echo "$*"|cut -d" " -f2-)
    [[ $type_of_msg == DEBUG ]] && [[ ${PRINT_DEBUG_MSGS} -ne 1 ]] && return
    [[ $type_of_msg == INFO ]] && type_of_msg="INFO " # one space for aligning
    [[ $type_of_msg == WARN ]] && type_of_msg="WARN " # as well

    # print to the terminal if we have one
    test -t 1 -o -n "$FORCE_LOG" && echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg $msg"
    echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg $msg" >> "${LOGFILE}"
}

doStatus(){
    if [[ -n "$BACKUP_SCHEDULE" ]]
    then
        type_of_msg=$(echo "$*"|cut -d" " -f1)
        orig_type_of_msg=${type_of_msg}
        msg=$(echo "$*"|cut -d" " -f2-)
        if [[ $type_of_msg == INFO ]]; then
            type_of_msg="INFO " # one space for aligning
            backup_dst_json="\"backup_path\":\"${BACKUP_LOCATION}/${BACKUPDIR}\","
            status="success"
        else
            status="failure"
        fi

        echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg $msg" > "${STATUSFILEPREFIX}${BACKUP_SCHEDULE}".log
        echo "{\"time\":\"$(date "+%Y-%m-%dT%H:%M:%SZ")\",\"level\":\"${orig_type_of_msg}\",\"status\":\"${status}\",${backup_dst_json}\"message\":\"${msg}\"}" > "${STATUSFILEPREFIX}${BACKUP_SCHEDULE}".json
    fi
}

error_exit()
{
    doLog "ERROR $1"
    exit 1
}

download_aws_backup() {
    BACKUP_URL=$1
    TARGET_DIR=$2
    doLog "INFO try to download with default bucket encryption"
    # shellcheck disable=SC2086
    /usr/bin/aws ${REGION_OPTION} s3 cp --debug --recursive --no-progress "${BACKUP_URL}" "${config[backup_path]}/${TARGET_DIR}" 2>&1 | /usr/local/bin/backup-log-filter.sh | tee -a "${LOGFILE}"
    ret_code=${PIPESTATUS[0]}

    if [[ "$ret_code" -ne "0" ]]
    then
        doLog "INFO try to download with aws:kms encryption"
        # shellcheck disable=SC2086
        /usr/bin/aws ${REGION_OPTION} s3 cp --debug --recursive --sse aws:kms --no-progress "${BACKUP_URL}" "${config[backup_path]}/${TARGET_DIR}" 2>&1 | /usr/local/bin/backup-log-filter.sh | tee -a "${LOGFILE}"
        ret_code=${PIPESTATUS[0]}
    fi

    if [[ "$ret_code" -ne "0" ]]
    then
        error_exit "Sync of backups from ${BACKUP_URL} failed!"
    else
        doLog "INFO Downloaded successfully"
    fi
}

validate_file_exists() {
  FILE=$1
  if [ ! -f "$FILE" ]; then
    error_exit "File $FILE not found!"
  fi
}

validate_header() {
  FILE=$1
  TYPE=$2

  validate_file_exists "$FILE"
  grep -q "type = $TYPE" $FILE || error_exit "Header file [$FILE] is missing 'type' line or is not set to [$TYPE]"
  grep -q "host = $(hostname -f)" $FILE || error_exit "Header file [$FILE] is missing 'host' line or is not set to [$(hostname -f)]"
}

validate_full_backup() {
  HEADER="${config[backup_path]}/${config[full_dir]}/header"
  TARFILE="${config[backup_path]}/${config[full_dir]}/ipa-full.tar"
  doLog "DEBUG Validate full back with header [$HEADER] and tar file [$TARFILE]"

  validate_header $HEADER "FULL"
  validate_file_exists $TARFILE
  doLog "INFO Full backup validated"
}

validate_data_backup() {
  HEADER="${config[backup_path]}/${config[data_dir]}/header"
  TARFILE="${config[backup_path]}/${config[data_dir]}/ipa-data.tar"
  doLog "DEBUG Validate data back with header [$HEADER] and tar file [$TARFILE]"

  validate_header $HEADER "DATA"
  validate_file_exists $TARFILE
  doLog "INFO Data backup validated"
}

# ON START
_prepare_locking

doLog "INFO Running IPA restore."
exlock_now || error_exit "A restore seems to be currently running. Lock file is at ${LOCKFILE}"

BACKUP_LOCATION="${config[backup_location]}/${BACKUP_PATH_POSTFIX}"

# shellcheck disable=SC2012
BACKUPDIR=$(basename "$(ls -td "${config[backup_path]}"/ipa-* | head -1)")

doLog "DEBUG Downloading backup from ${FULL_BACKUP_LOCATION} and ${DATA_BACKUP_LOCATION} on ${config[backup_platform]}"

if [[ "${config[backup_platform]}" = "AWS" ]]; then
    doLog "INFO Syncing backups from AWS S3"

    REGION_OPTION=""
    if [[ -n "${config[aws_region]}" ]]; then
      if [[ -n "${config[aws_endpoint]}" ]]; then
        doLog "INFO Using --endpoint-url for download from S3"
        REGION_OPTION="--endpoint-url ${config[aws_endpoint]} --region ${config[aws_region]}"
      else
        doLog "INFO Using only --region for download from S3"
        REGION_OPTION="--region ${config[aws_region]}"
      fi
    fi
    download_aws_backup "${FULL_BACKUP_LOCATION}" "${config[full_dir]}"
    download_aws_backup "${DATA_BACKUP_LOCATION}" "${config[data_dir]}"
elif [[ "${config[backup_platform]}" = "AZURE" ]]; then
    doLog "INFO Syncing backups from Azure Blog Storage"
    /bin/keyctl new_session 2>&1 | tee -a "${LOGFILE}"
    if [[ "${PIPESTATUS[0]}" -ne "0" ]]; then
        SESSION_PERMS=$(/bin/keyctl rdescribe @s)
        SESSION_OWNER=$(echo "${SESSION_PERMS}" | cut -f2 -d";")
        if [[ "${SESSION_OWNER}" != "${UID}" ]]; then
            error_exit "Unable to setup keyring session. The keyring session permissions are ${SESSION_PERMS} but the UID is ${UID}. Was this command run with \"sudo\" or \"sudo su\"? If so, then try \"sudo su -\"."
        else
            error_exit "Unable to setup keyring session"
        fi
    fi
    /usr/local/bin/azcopy login --identity --identity-resource-id "${config[azure_instance_msi]}" 2>&1 | tee -a "${LOGFILE}"
    if [[ "${PIPESTATUS[0]}" -ne "0" ]]; then
        error_exit "Unable to login to Azure!"
    fi
    /usr/local/bin/azcopy copy "${FULL_BACKUP_LOCATION}/*" "${config[backup_path]}/${config[full_dir]}" --recursive=true --check-length=false 2>&1 | tee -a "${LOGFILE}"
    if [[ "${PIPESTATUS[0]}" -ne "0" ]]; then
        error_exit "Sync of backups from ${FULL_BACKUP_LOCATION} failed!"
    fi
    /usr/local/bin/azcopy copy "${DATA_BACKUP_LOCATION}/*" "${config[backup_path]}/${config[data_dir]}" --recursive=true --check-length=false 2>&1 | tee -a "${LOGFILE}"
    if [[ "${PIPESTATUS[0]}" -ne "0" ]]; then
        error_exit "Sync of backups from ${DATA_BACKUP_LOCATION} failed!"
    fi
elif [[ "${config[backup_platform]}" = "GCP" ]]; then
    mkdir -p "${config[backup_path]}/${config[full_dir]}"
    /bin/gsutil cp "${FULL_BACKUP_LOCATION}/*" "${config[backup_path]}/${config[full_dir]}" >> "${LOGFILE}" 2>&1 || error_exit "Sync of backups from ${FULL_BACKUP_LOCATION} failed!"
    mkdir -p "${config[backup_path]}/${config[data_dir]}"
    /bin/gsutil cp "${DATA_BACKUP_LOCATION}/*" "${config[backup_path]}/${config[data_dir]}" >> "${LOGFILE}" 2>&1 || error_exit "Sync of backups from ${DATA_BACKUP_LOCATION} failed!"
fi

validate_full_backup
validate_data_backup

doStatus "INFO Backup download and validation succeeded."

