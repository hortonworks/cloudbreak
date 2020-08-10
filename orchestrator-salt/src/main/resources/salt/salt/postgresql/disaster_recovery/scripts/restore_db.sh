#!/bin/bash
# restore_db.sh
# This script uses the 'psql' cli to drop hive and ranger databases, create them, then read in a plain SQL file to restore data.
# We use the `azcopy copy` and `aws s3 cp` commands to copy backups from Azure or AWS respectively.
# When using either cloud provider, the plaintext SQL files are first copied to the local filesystem, then fed into psql.

set -o errexit
set -o nounset
set -o pipefail

if [[ $# -ne 6 && $# -ne 7 ]]; then
  echo "Invalid inputs provided"
  echo "Script accepts 5 inputs:"
  echo "  1. Cloud Provider (azure | aws)"
  echo "  2. Object Storage Service url to retrieve backups."
  echo "  3. PostgreSQL host name."
  echo "  4. PostgreSQL port."
  echo "  5. PostgreSQL user name."
  echo "  6. AWS Region option."
  echo "  7. ranger admin group"
  echo "  8. (optional) Log file location."
  exit 1
fi

CLOUD_PROVIDER=$(echo "$1" | tr '[:upper:]' '[:lower:]')
CLOUD_LOCATION=$(echo "$2" | sed 's/\/\+$//g') # Clear trailng '/' (if present) for later path joining.
HOST="$3"
PORT="$4"
USERNAME="$5"

REGION_OPTION=${6:-''}
if [[ -n $REGION_OPTION ]]; then
  # todo: This only works with AWS!
  REGION_OPTION="--region ${REGION_OPTION}"
fi

RANGERGROUP="$7"
LOGFILE=${8:-/var/log/}/dl_postgres_restore.log
echo "Logs at ${LOGFILE}"

BACKUPS_DIR="/var/tmp/postgres_restore_staging"

doLog() {
  type_of_msg=$(echo "$@" | cut -d" " -f1)
  msg=$(echo "$*" | cut -d" " -f2-)
  [[ $type_of_msg == INFO ]] && type_of_msg="INFO " # one space for aligning
  [[ $type_of_msg == WARN ]] && type_of_msg="WARN " # as well

  # print to the terminal if we have one
  test -t 1 && echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg"
  echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg" >>$LOGFILE
}

errorExit() {
  doLog "ERROR $1"
  exit 1
}

replace_ranger_group_before_import() {
  doLog "INFO Replacing RANGER_WAG with "$1" in the dump before import"
  echo "sed --in-place="original" 's/RANGER_WAG/"$1"/g' $2"
  ret_code=$(sed --in-place="original" -e s/RANGER_WAG/"$1"/g "$2" || echo $?)
  if [[ -n "$ret_code" ]] && [[ "$ret_code" -ne 0 ]]; then
    errorExit "Unable to re-write file $2"
  fi
}

restore_db_from_local() {
  SERVICE=$1
  BACKUP="${BACKUPS_DIR}/${SERVICE}_backup"

  if [[ "$SERVICE" == "ranger" ]]; then
    replace_ranger_group_before_import $RANGERGROUP $BACKUP
  fi  
  doLog "INFO Restoring $SERVICE"

  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "drop database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to drop database ${SERVICE}"
  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "create database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to re-create database ${SERVICE}"
  psql --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" < "$BACKUP" > $LOGFILE 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore ${SERVICE}"
  doLog "INFO Succesfully restored ${SERVICE}"
}

run_azure_restore() {
  mkdir -p "$BACKUPS_DIR"
  azcopy login --identity > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Could not login to Azure"
  azcopy copy "$CLOUD_LOCATION"/* "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Could not copy backups from Azure."
  restore_db_from_local "hive"
  restore_db_from_local "ranger"
  rm -rfv "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

run_aws_restore () {
  mkdir -p "$BACKUPS_DIR"
  # shellcheck disable=SC2086
  # We want to allow word splitting for the REGION_OPTION
  aws ${REGION_OPTION} s3 cp "${CLOUD_LOCATION}/" "$BACKUPS_DIR" --recursive > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Could not copy backups from AWS S3."
  restore_db_from_local "hive"
  restore_db_from_local "ranger"
  rm -rfv "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

doLog "Info Ranger admin group is $7"
doLog "INFO Initiating restore"
if [[ "$CLOUD_PROVIDER" == "azure" ]]; then
  run_azure_restore
elif [[ "$CLOUD_PROVIDER" == "aws" ]]; then
  run_aws_restore
else
  errorExit "Unknown cloud provider: ${CLOUD_PROVIDER}"
fi

doLog "INFO Completed restore."
