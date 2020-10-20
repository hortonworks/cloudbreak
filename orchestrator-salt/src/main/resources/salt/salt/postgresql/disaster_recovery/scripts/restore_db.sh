#!/bin/bash
# restore_db.sh
# This script uses the 'psql' cli to drop hive and ranger databases, create them, then read in a plain SQL file to restore data.
# We retrieve the SQL files from a valid URL, which should be `s3a://` or `abfs://` for AWS or Azure clouds respectively.

set -o errexit
set -o nounset
set -o pipefail
set -o xtrace

if [[ $# -ne 5 && $# -ne 6 ]]; then
  echo "Invalid inputs provided"
  echo "Script accepts 5 inputs:"
  echo "  1. Object Storage Service url to retrieve backups."
  echo "  2. PostgreSQL host name."
  echo "  3. PostgreSQL port."
  echo "  4. PostgreSQL user name."
  echo "  5. Ranger admin group."
  echo "  6. (optional) Log file location."
  exit 1
fi

BACKUP_LOCATION="$1/*" # Trailing slash and glob so we copy the _items_ in the directory not the directory itself.
HOST="$2"
PORT="$3"
USERNAME="$4"
RANGERGROUP="$5"
LOGFILE=${6:-/var/log/}/dl_postgres_restore.log
echo "Logs at ${LOGFILE}"

BACKUPS_DIR="/var/tmp/postgres_restore_staging"

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

replace_ranger_group_before_import() {
  doLog "INFO Replacing _RANGER_WAG_2f0264fa-0a04-462b-af85-7c09891568ef with "$1" in the dump before import"
  ret_code=$(sed --in-place="original" -e s/_RANGER_WAG_2f0264fa-0a04-462b-af85-7c09891568ef/"$1"/g "$2" || echo $?)
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
  psql --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" <"$BACKUP" >$LOGFILE 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore ${SERVICE}"
  doLog "INFO Succesfully restored ${SERVICE}"
}

run_restore() {
  mkdir -p "$BACKUPS_DIR"
  kinit_as_hdfs
  hdfs dfs -copyToLocal -f "$BACKUP_LOCATION" "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Could not copy backups from ${BACKUP_LOCATION}."

  restore_db_from_local "hive"
  restore_db_from_local "ranger"

  rm -rfv "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

doLog "Info Ranger admin group is $RANGERGROUP"
doLog "INFO Initiating restore"
run_restore
doLog "INFO Completed restore."
