#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

if [ $# -ne 5 ] || [ $# -ne 6 ]; then
  echo "Invalid inputs provided"
  echo "Script accepts 5 inputs:"
  echo "  1. Cloud Provider (azure | aws)"
  echo "  2. Object Storage Service url to retrieve backups."
  echo "  3. PostgreSQL host name."
  echo "  4. PostgreSQL port."
  echo "  5. PostgreSQL user name."
  echo
  echo "  Optional: PostgreSQL password."
  exit 1
fi

CLOUD_PROVIDER="$1"
HOST="$2"
PORT="$3"
CLOUD_LOCATION=$(echo "$4"| sed "s/\/\+$//g") # Clear trailng '/' (if present) for later path joining.
USERNAME="$5"
export PGPASSWORD="$6" # We can provide the password to pg_dump through this variable, or in ~/.pgpass

LOGFILE=/var/log/dl_postgres_restore.log
echo "Logs at ${LOGFILE}"

BACKUPS_DIR="/var/tmp/postgres_restore_staging"

doLog() {
  type_of_msg=$(echo $* | cut -d" " -f1)
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

restore_db_from_local() {
  SERVICE=$1
  BACKUP="${BACKUPS_DIR}/${SERVICE}_backup"
  LIST_FILE="${BACKUPS_DIR}/${SERVICE}_list"

  doLog "INFO Restoring $SERVICE"
  pg_restore -l "$BACKUP" | sed -e "s/\(.*0 0 COMMENT - EXTENSION plpgsql\)/;\1/" >"$LIST_FILE" # We have to drop a problematic COMMENT added by RDS backups.
  pg_restore --clean --if-exists --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" --use-list="$LIST_FILE" "$BACKUP" >>$LOGFILE 2>&1 || errorExit "Unable to restore ${SERVICE}"
  doLog "INFO Succesfully restored ${SERVICE}"
}

run_azure_restore() {
  mkdir -p "$BACKUPS_DIR"
  azcopy login --identity >>$LOGFILE 2>&1 || errorExit "Could not login to Azure"
  azcopy copy "$CLOUD_LOCATION"/* "$BACKUPS_DIR" >>$LOGFILE 2>&1 || errorExit "Could not copy backups from Azure."
  restore_db_from_local "hive"
  restore_db_from_local "ranger"
  rm -rfv "$BACKUPS_DIR" >>$LOGFILE 2>&1
}

restore_db_from_s3() {
  SERVICE="$1"
  BACKUP="${BACKUPS_DIR}/${SERVICE}_backup"
  LIST_FILE="${BACKUPS_DIR}/${SERVICE}_list"

  doLog "INFO Copying ${CLOUD_LOCATION}/${SERVICE}_backup"
  aws s3 cp "${CLOUD_LOCATION}/${SERVICE}_backup" "$BACKUPS_DIR" || errorExit "Could not copy $SERVICE backup from AWS S3."

  doLog "INFO Restoring $SERVICE"
  pg_restore -l "$BACKUP" | sed -e "s/\(.*0 0 COMMENT - EXTENSION plpgsql\)/;\1/" >"$LIST_FILE" # We have to drop a problematic COMMENT added by RDS backups.
  pg_restore --clean --if-exists --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" --use-list="$LIST_FILE" "$BACKUP" >>$LOGFILE 2>&1 || errorExit "Unable to restore ${SERVICE}"
  doLog "INFO Succesfully restored ${SERVICE}"
}

run_aws_restore () {
  mkdir -p "$BACKUPS_DIR"
  restore_db_from_s3 "hive"
  restore_db_from_s3 "ranger"
  rm -rfv "$BACKUPS_DIR" >>$LOGFILE 2>&1
}

if [[ "$CLOUD_PROVIDER" == "azure" ]]; then
  run_azure_restore
elif [[ "$CLOUD_PROVIDER" == "aws" ]]; then
  run_aws_restore
else
  errorExit "Unknown cloud provider: ${CLOUD_PROVIDER}"
fi

doLog "INFO Completed restore."
