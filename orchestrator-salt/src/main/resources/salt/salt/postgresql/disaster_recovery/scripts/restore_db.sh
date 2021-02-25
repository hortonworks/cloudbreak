#!/bin/bash
# restore_db.sh
# This script uses the 'psql' cli to drop hive and ranger databases, create them, then read in a plain SQL file to restore data.
# We retrieve the SQL files from a valid URL, which should be `s3a://` or `abfs://` for AWS or Azure clouds respectively.

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

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

doLog() {
  set +x
  type_of_msg=$(echo "$@" | cut -d" " -f1)
  msg=$(echo "$*" | cut -d" " -f2-)
  [[ $type_of_msg == INFO ]] && type_of_msg="INFO   " # three space for aligning
  [[ $type_of_msg == WARN ]] && type_of_msg="WARN   " # three space for aligning
  [[ $type_of_msg == ERROR ]] && type_of_msg="ERROR " # one space for aligning

  # print to the terminal if we have one
  test -t 1 && echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg"
  echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg" >>$LOGFILE
  set -x
}

errorExit() {
  doLog "ERROR $1"
  exit 1
}

kinit_as() {
  doLog "attempting kinit as $1 using Keytab: $2"
  kinit -kt "$2" "$1/$(hostname -f)"
  if [[ $? -ne 0 ]]; then
    doLog "Couldn't kinit as $1"
    return 1
  fi
}

run_kinit() {
  export KRB5CCNAME=/tmp/krb5cc_cloudbreak_$EUID

  HDFS_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -path "*hdfs*" | head -n 1)
  HBASE_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -a \( -path "*hbase-REGIONSERVER*" -o -path "*hbase-MASTER*" \) -a -type f | head -n 1)
  SOLR_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -path "*solr-SOLR_SERVER*" | head -n 1)
  ATLAS_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -path "*atlas-ATLAS_SERVER*" | head -n 1)

  if kinit_as hdfs "$HDFS_KEYTAB"; then
    doLog "Successful kinit using hdfs principal"
  elif kinit_as hbase "$HBASE_KEYTAB"; then
    doLog "Successful kinit using hbase principal"
  elif kinit_as solr "$SOLR_KEYTAB"; then
    doLog "Successful kinit using solr principal"
  elif kinit_as atlas "$ATLAS_KEYTAB"; then
    doLog "Successful kinit using atlas principal"
  else
    errorExit "Couldn't get kerberos ticket to access cloud storage."
  fi
}

replace_ranger_group_before_import() {
  doLog "INFO Replacing _RANGER_WAG_2f0264fa-0a04-462b-af85-7c09891568ef with "$1" in the dump before import"
  ret_code=$(sed --in-place="original" -e s/_RANGER_WAG_2f0264fa-0a04-462b-af85-7c09891568ef/"$1"/g "$2" || echo $?)
  if [[ -n "$ret_code" ]] && [[ "$ret_code" -ne 0 ]]; then
    errorExit "Unable to re-write file $2"
  fi
}

close_existing_connections() {
  SERVICE=$1
  doLog "INFO Closing existing connections to ${SERVICE}"
  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '${SERVICE}' AND pid <> pg_backend_pid();" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to close connections to ${SERVICE}"
}

limit_incomming_connection() {
  SERVICE=$1
  COUNT=$2
  doLog "INFO limit existing connections to ${COUNT}"
  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "alter user ${SERVICE} connection limit ${COUNT};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to limit connections to ${SERVICE}"
}

restore_db_from_local() {
  SERVICE=$1
  BACKUP="${BACKUPS_DIR}/${SERVICE}_backup"

  if [[ "$SERVICE" == "ranger" ]]; then
    replace_ranger_group_before_import $RANGERGROUP $BACKUP
  fi
  limit_incomming_connection $SERVICE 0
  close_existing_connections $SERVICE
  doLog "INFO Restoring $SERVICE"

  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "drop database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to drop database ${SERVICE}"
  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "create database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to re-create database ${SERVICE}"
  psql --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" <"$BACKUP" >$LOGFILE 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore ${SERVICE}"
  doLog "INFO Succesfully restored ${SERVICE}"

  limit_incomming_connection $SERVICE -1
}

run_restore() {
  mkdir -p "$BACKUPS_DIR"
  run_kinit

  if [[ $BACKUP_LOCATION == s3a://* ]] ;
  then
    STRIPPED_S3_URL=$(echo "$BACKUP_LOCATION" | sed -e 's/[a-fA-F0-9]\{8\}-[a-fA-F0-9]\{4\}-[a-fA-F0-9]\{4\}-[a-fA-F0-9]\{4\}-[a-fA-F0-9]\{12\}_database_backup\/*\*\?$//g')
    doLog "Begining to perform s3guard import for ${STRIPPED_S3_URL}"
    hadoop s3guard import "$STRIPPED_S3_URL" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || doLog "ERROR Unable to import S3Guard metadata."
    doLog "s3guard import is complete"
  fi

  hdfs dfs -copyToLocal -f "$BACKUP_LOCATION" "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Could not copy backups from ${BACKUP_LOCATION}."

  restore_db_from_local "hive"
  restore_db_from_local "ranger"

  rm -rfv "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

doLog "Info Ranger admin group is $RANGERGROUP"
doLog "INFO Initiating restore"
run_restore
doLog "INFO Completed restore."
