#!/bin/bash
# backup_db.sh
# This script uses the 'pg_dump' utility to dump the contents of hive and ranger PostgreSQL databases as plain SQL.
# After PostgreSQL contents are dumped, the SQL file is uploaded using the hdfs cli.
# For AWS or Azure, the url should start with `s3a://` or `abfs://`, respectively.

set -o errexit
set -o nounset
set -o pipefail
set -o xtrace

if [[ $# -ne 5 && $# -ne 6 ]]; then
  echo "Invalid inputs provided"
  echo "Script accepts 5 inputs:"
  echo "  1. Object Storage Service url to place backups."
  echo "  2. PostgreSQL host name."
  echo "  3. PostgreSQL port."
  echo "  4. PostgreSQL user name."
  echo "  5. Ranger admin group."
  echo "  6. (optional) Log file location."
  exit 1
fi

BACKUP_LOCATION="$1"
HOST="$2"
PORT="$3"
USERNAME="$4"
RANGERGROUP="$5"
LOGFILE=${6:-/var/log/}/dl_postgres_backup.log
echo "Logs at ${LOGFILE}"

if [[ -f /hadoopfs/fs1/database-cacerts/certs.pem ]]; then
  export PGSSLROOTCERT=/hadoopfs/fs1/database-cacerts/certs.pem
  export PGSSLMODE=verify-full
fi

doLog() {
  set +x
  type_of_msg=$(echo "$@" | cut -d" " -f1)
  msg=$(echo "$*" | cut -d" " -f2-)
  [[ $type_of_msg == INFO ]] && type_of_msg="INFO " # one space for aligning
  [[ $type_of_msg == WARN ]] && type_of_msg="WARN " # as well

  # print to the terminal if we have one
  test -t 1 && echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg"
  echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg" >>$LOGFILE
  set -x
}

errorExit() {
  set +x
  doLog "ERROR $1"
  exit 1
}

kinit_as_hdfs() {
  # Use a new Kerberos credential cache, to keep from clobbering the default.
  export KRB5CCNAME=/tmp/krb5cc_cloudbreak_$EUID
  HDFS_KEYTAB=/run/cloudera-scm-agent/process/$(ls -t /run/cloudera-scm-agent/process/ | grep hdfs-NAMENODE$ | head -n 1)/hdfs.keytab
  doLog "kinit as hdfs using Keytab: $HDFS_KEYTAB"
  kinit -kt "$HDFS_KEYTAB" "hdfs/$(hostname -f)"
  if [[ $? -ne 0 ]]; then
    errorExit "Couldn't kinit as hdfs"
  fi
}

replace_ranger_group_before_export() {
  doLog "Ranger admin group is $RANGERGROUP"
  doLog "INFO Replacing "$1" with _RANGER_WAG_2f0264fa-0a04-462b-af85-7c09891568ef in the dump before export"
  ret_code=$(sed --in-place="_original" -e s/"$1"/_RANGER_WAG_2f0264fa-0a04-462b-af85-7c09891568ef/g "$2" || echo $?)
  if [[ -n "$ret_code" ]] && [[ "$ret_code" -ne 0 ]]; then
    errorExit "Unable to re-write file $2"
  else
    rm "${2}_original"
  fi
}

move_backup_to_cloud () {
  LOCAL_BACKUP="$1"
  kinit_as_hdfs
  doLog "INFO Uploading to ${BACKUP_LOCATION}"

  hdfs dfs -mkdir -p "$BACKUP_LOCATION"
  OBJECT_STORE_PATH="${BACKUP_LOCATION}/${SERVICE}_backup"
  hdfs dfs -moveFromLocal "$LOCAL_BACKUP" "$OBJECT_STORE_PATH" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to upload $SERVICE backup"
  doLog "INFO Completed upload to ${BACKUP_LOCATION}"
}

backup_database_for_service() {
  SERVICE="$1"
  doLog "INFO Dumping ${SERVICE}"
  LOCAL_BACKUP=${DATE_DIR}/${SERVICE}_backup
  pg_dump --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="$SERVICE" --format=plain --file="$LOCAL_BACKUP" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump ${SERVICE}"

  if [[ "$SERVICE" == "ranger" ]]; then
    replace_ranger_group_before_export $RANGERGROUP $LOCAL_BACKUP
  fi

  move_backup_to_cloud "$LOCAL_BACKUP"

  doLog "INFO Completed upload to ${BACKUP_LOCATION}"
  doLog "INFO ${SERVICE} dumped to ${OBJECT_STORE_PATH}"
}

run_backup() {
  BACKUPS_DIR="/var/tmp/"
  DATE_DIR=${BACKUPS_DIR}/$(date '+%Y-%m-%dT%H:%M:%SZ')
  mkdir -p "$DATE_DIR" || error_exit "Could not create local directory for backups."

  backup_database_for_service "hive"
  backup_database_for_service "ranger"

  rmdir -v "$DATE_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

doLog "INFO Starting backup to ${BACKUP_LOCATION}"
run_backup
doLog "INFO Completed backup ${BACKUP_LOCATION}"
