#!/bin/bash
# backup_db.sh
# This script uses the 'pg_dump' utility to dump the contents of hive and ranger PostgreSQL databases as plain SQL.
# After PostgreSQL contents are dumped, the SQL file is uploaded using the hdfs cli.
# For AWS or Azure, the url should start with `s3a://` or `abfs://`, respectively.

set -o nounset
set -o pipefail
set -o xtrace

if [[ $# -lt 6 || $# -gt 7 || "$1" == "None" ]]; then
  echo "Invalid inputs provided"
  echo "Script accepts at least 6 and at most 7 inputs:"
  echo "  1. Object Storage Service url to place backups."
  echo "  2. PostgreSQL host name."
  echo "  3. PostgreSQL port."
  echo "  4. PostgreSQL user name."
  echo "  5. Ranger admin group."
  echo "  6. Whether or not to close connections for the database while it is being backed up."
  echo "  7. (optional) Name of the database to backup. If not given, will backup ranger and hive databases."
  exit 1
fi

BACKUP_LOCATION="$1"
HOST="$2"
PORT="$3"
USERNAME="$4"
RANGERGROUP="$5"
CLOSECONNECTIONS="$6"
DATABASENAME="${7-}"
LOGFILE=/var/log/dl_postgres_backup.log
echo "Logs at ${LOGFILE}"

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

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
  KNOX_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -path "*knox-KNOX_GATEWAY*" | head -n 1)

  if kinit_as hdfs "$HDFS_KEYTAB"; then
    doLog "Successful kinit using hdfs principal"
  elif kinit_as hbase "$HBASE_KEYTAB"; then
    doLog "Successful kinit using hbase principal"
  elif kinit_as solr "$SOLR_KEYTAB"; then
    doLog "Successful kinit using solr principal"
  elif kinit_as atlas "$ATLAS_KEYTAB"; then
    doLog "Successful kinit using atlas principal"
  elif kinit_as knox "$KNOX_KEYTAB"; then
    doLog "Successful kinit using knox principal"
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
  run_kinit
  doLog "INFO Uploading to ${BACKUP_LOCATION}"

  hdfs dfs -mkdir -p "$BACKUP_LOCATION"
  OBJECT_STORE_PATH="${BACKUP_LOCATION}/${SERVICE}_backup"
  hdfs dfs -moveFromLocal -f "$LOCAL_BACKUP" "$OBJECT_STORE_PATH" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to upload $SERVICE backup"
  doLog "INFO Completed upload to ${BACKUP_LOCATION}"
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

is_database_exists() {
  SERVICE=$1
  doLog "INFO Checking the existence of database ${SERVICE}"
  database_name=$(psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "SELECT datname FROM pg_catalog.pg_database WHERE datname = '$SERVICE';" -At > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to check the existence of database $SERVICE")
  if [[ "$database_name" != "$SERVICE" ]];then
    return 1
  fi
  return 0
}

backup_database_for_service() {
  SERVICE="$1"
  is_database_exists $SERVICE
  if [ "$?" -eq 1 ];then
    doLog "WARN database for $SERVICE doesn't exists"
    return 0
  fi

  if [[ "$CLOSECONNECTIONS" == "true" ]]; then
    limit_incomming_connection $SERVICE 0
    close_existing_connections $SERVICE
  fi

  doLog "INFO Dumping ${SERVICE}"
  LOCAL_BACKUP=${DATE_DIR}/${SERVICE}_backup
  pg_dump --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="$SERVICE" --format=plain --file="$LOCAL_BACKUP" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump ${SERVICE}"
  if [[ "$CLOSECONNECTIONS" == "true" ]]; then
    limit_incomming_connection $SERVICE -1
  fi

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

  doLog "INFO Conditional variable for closing connections to database during backup is set to ${CLOSECONNECTIONS}"

  if [[ -z "$DATABASENAME" ]]; then
    doLog "INFO No database name provided. Will backup hive, ranger, profiler_agent and profiler_metric databases."
    backup_database_for_service "hive"
    backup_database_for_service "ranger"
    backup_database_for_service "profiler_agent"
    backup_database_for_service "profiler_metric"
  else
    doLog "INFO Backing up ${DATABASENAME}."
    backup_database_for_service "${DATABASENAME}"
  fi

  rmdir -v "$DATE_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

doLog "INFO Starting backup to ${BACKUP_LOCATION}"
run_backup
doLog "INFO Completed backup ${BACKUP_LOCATION}"
