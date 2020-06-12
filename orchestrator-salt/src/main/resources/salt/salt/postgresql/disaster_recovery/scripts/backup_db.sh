#!/bin/bash
# backup_db.sh
# This script uses the 'pg_dump' utility to dump the contents of hive and ranger PostgreSQL databases as plain SQL.
# After PostgreSQL contents are dumped, the SQL is uploaded to AWS or Azure using their CLI clients, 'aws s3 cp' and 'azcopy copy' respectively.
# For the Azure upload, we must write the SQL commands to a local disk before running the `azcopy copy` command.
# For AWS, the cli supports piping file contents to stdin, so we do that and avoid writing to a local file.

set -o errexit
set -o nounset
set -o pipefail

if [ $# -ne 5 ]; then
  echo "Invalid inputs provided"
  echo "Script accepts 5 inputs:"
  echo "  1. Cloud Provider (azure | aws)"
  echo "  2. Object Storage Service url to place backups."
  echo "  3. PostgreSQL host name."
  echo "  4. PostgreSQL port."
  echo "  5. PostgreSQL user name."
  exit 1
fi

CLOUD_PROVIDER=$(echo "$1" | tr '[:upper:]' '[:lower:]')
BACKUP_LOCATION=$(echo "$2" | sed 's/\/\+$//g') # Clear trailng '/' (if present) for later path joining.
HOST="$3"
PORT="$4"
USERNAME="$5"

LOGFILE=/var/log/dl_postgres_backup.log

echo "Logs at ${LOGFILE}"

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

dump_to_azure() {
  SERVICE="$1"
  doLog "INFO Dumping ${SERVICE}"
  LOCAL_BACKUP=${DATE_DIR}/${SERVICE}_backup
  pg_dump --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="$SERVICE" --format=plain --file="$LOCAL_BACKUP" >>$LOGFILE 2>&1 || errorExit "Unable to dump ${SERVICE}"

  doLog "INFO Uploading to ${BACKUP_LOCATION}"
  AZURE_LOCATION="${BACKUP_LOCATION}/${SERVICE}_backup"
  azcopy copy "$LOCAL_BACKUP" "$AZURE_LOCATION" >>$LOGFILE 2>&1 || errorExit "Unable to upload $SERVICE backup"
  doLog "INFO Completed upload to ${BACKUP_LOCATION}"

  rm -v "$LOCAL_BACKUP" >>$LOGFILE 2>&1

  doLog "INFO ${SERVICE} dumped to "
}
run_azure_backup () {
  BACKUPS_DIR="/var/tmp/"
  DATE_DIR=${BACKUPS_DIR}/$(date '+%Y-%m-%dT%H:%M:%SZ')
  mkdir -p "$DATE_DIR" || error_exit "Could not create local directory for backups."

  azcopy login --identity || errorExit "Could not login to Azure"
  dump_to_azure "hive"
  dump_to_azure "ranger"

  rmdir -v "$DATE_DIR" >>$LOGFILE 2>&1
}

dump_to_s3() {
  SERVICE=$1
  S3_LOCATION="${BACKUP_LOCATION}/${SERVICE}_backup"
  doLog "INFO Dumping ${SERVICE} to ${S3_LOCATION}"
  pg_dump --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="${SERVICE}" --format=plain 2>>$LOGFILE | /usr/bin/aws s3 cp --sse AES256 --no-progress - "${S3_LOCATION}" 2>>$LOGFILE || errorExit "Unable to dump ${SERVICE}."
  doLog "INFO ${SERVICE} dumped to ${S3_LOCATION}"
}
run_aws_backup () {
  dump_to_s3 "hive"
  dump_to_s3 "ranger"
}

doLog "INFO Starting backup to ${BACKUP_LOCATION}"

if [[ "$CLOUD_PROVIDER" = "azure" ]]; then
  run_azure_backup
elif [[ "$CLOUD_PROVIDER" = "aws" ]]; then
  run_aws_backup
else
  errorExit "Unknown cloud provider: ${CLOUD_PROVIDER}"
fi

doLog "INFO Completed backup ${BACKUP_LOCATION}"
