#!/bin/bash
# restore_db.sh
# This script uses the 'psql' cli to drop hive and ranger databases, create them, then read in a plain SQL file to restore data.
# We retrieve the SQL files from a valid URL, which should be `s3a://` or `abfs://` for AWS or Azure clouds respectively.

set -o nounset
set -o pipefail

LOGFILE=/var/log/dl_postgres_restore.log

echo "Logs are at ${LOGFILE}"

exec 3>&1 4>&2
trap 'exec 2>&4 1>&3' 0 1 2 3
exec 1> >(tee -a "${LOGFILE}") 2> >(tee -a "${LOGFILE}" >&2)

doLog() {
  type_of_msg=$(echo "$@" | cut -d" " -f1)
  msg=$(echo "$*" | cut -d" " -f2-)
  [[ $type_of_msg == INFO ]] && type_of_msg="INFO   " # three space for aligning
  [[ $type_of_msg == WARN ]] && type_of_msg="WARN   " # three space for aligning
  [[ $type_of_msg == ERROR ]] && type_of_msg="ERROR " # one space for aligning

  # print to the terminal if we have one
  test -t 1 && echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg"
  echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg" >>$LOGFILE
}

validate_directory() {
  local dir_path="$1"

  if [ ! -d "$dir_path" ]; then
    doLog "ERROR Directory '$dir_path' does not exist"
    return 1
  fi

  if [ ! -r "$dir_path" ]; then
    doLog "ERROR Directory '$dir_path' is not readable"
    return 2
  fi

  if ! touch "$dir_path/.test_write_$$" 2>/dev/null; then
    doLog "ERROR Cannot write to directory '$dir_path'"
    return 4
  fi

  rm -f "$dir_path/.test_write_$$" 2>/dev/null

  doLog "INFO Directory validation successful: $dir_path"
  return 0
}

if [[ $# -lt 5 || $# -gt 7 || "$1" == "None" || -z "$1" ]]; then
  doLog "ERROR: Invalid inputs provided"
  doLog "A total of $# inputs were provided."
  if [[ $# -gt 0 ]]; then
    doLog "Below are the inputs passed in. If some of them are empty, it could be incorrect:"
    COUNTER=1
    for input in "$@"
    do
      echo "  $COUNTER. $input"
      let COUNTER++
    done
  fi
  doLog "There might be missing values in /srv/pillar/postgresql/disaster_recovery.sls or /srv/pillar/postgresql/postgre.sls."
  doLog "This might be caused by the command not being run on the Primary Gateway node or due to never having run a backup/restore via the CDP CLI before."
  doLog "Script accepts at least 5 and at most 7 inputs:"
  doLog "  1. Object Storage Service url to retrieve backups."
  doLog "  2. PostgresSQL host name."
  doLog "  3. PostgresSQL port."
  doLog "  4. PostgresSQL user name."
  doLog "  5. Ranger admin group."
  doLog "  6. (optional) Name of the database to restore. If not given or 'DEFAULT', will restore ranger and hive databases."
  doLog "  7. (optional) Local backup base directory. If not given, will use /var/tmp."
  exit 1
fi

BACKUP_LOCATION="$1/*" # Trailing slash and glob so we copy the _items_ in the directory not the directory itself.
HOST="$2"
PORT="$3"
USERNAME="$4"
RANGERGROUP="$5"
DATABASENAME="${6-}"
LOCAL_BACKUP_BASE_DIR="${7:-/var/tmp}"

if ! validate_directory "$LOCAL_BACKUP_BASE_DIR"; then
  doLog "ERROR Local backup directory is not valid"
  exit 1
fi

# Script appends its own suffix
BACKUPS_DIR="${LOCAL_BACKUP_BASE_DIR}/postgres_restore_staging"

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE="{{ postgresql.ssl_verification_mode }}"
{%- endif %}

errorExit() {
  if [[ -z "$DATABASENAME" || "$DATABASENAME" == "DEFAULT" ]]; then
    limit_incomming_connection "hive" -1
    limit_incomming_connection "ranger" -1
    limit_incomming_connection "profiler_agent" -1
    limit_incomming_connection "profiler_metric" -1
    limit_incomming_connection "knox_gateway" -1
  else
    for db in $DATABASENAME; do
      doLog "Limiting incomming connections to ${db}."
      limit_incomming_connection "${db}" -1
    done
  fi

  if [ -d "$BACKUPS_DIR" ] && [[ $BACKUPS_DIR == */postgres_restore_staging* || $BACKUPS_DIR == */postgres_backup_staging* ]]; then
      rm -rfv "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
      doLog "Removed directory $BACKUPS_DIR"
  fi
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
  else
    errorExit "Unable to kinit as any principal, something is wrong with the Kerberos connection. Please check logs for additional info."
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

is_database_exists() {
  SERVICE=$1
  doLog "INFO Checking the existence of database ${SERVICE}"
  database_name=$(psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "SELECT datname FROM pg_catalog.pg_database WHERE datname = '$SERVICE';" -At > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to check the existence of database $SERVICE")
  if [[ "$database_name" != "$SERVICE" ]];then
    return 1
  fi
  return 0
}

restore_db_from_local() {
  SERVICE=$1
  BACKUP_PLAIN="${BACKUPS_DIR}/${SERVICE}_backup"
  BACKUP_DUMP="${BACKUPS_DIR}/${SERVICE}_backup.dump"

  is_database_exists $SERVICE
  if [ "$?" -eq 1 ];then
    doLog "WARN database for $SERVICE doesn't exist"
    return 0
  fi

  if [[ ! -f "$BACKUP_PLAIN" && ! -f "$BACKUP_DUMP" ]]; then
    doLog "INFO Not restoring ${SERVICE} as neither ${BACKUP_PLAIN} nor ${BACKUP_DUMP} files exist"
    return 0
  fi

  if [[ "$SERVICE" == "ranger" ]]; then
    # Ranger databases are always plain backup files
    replace_ranger_group_before_import $RANGERGROUP $BACKUP_PLAIN
  fi

  limit_incomming_connection $SERVICE 0
  close_existing_connections $SERVICE
  doLog "INFO Restoring $SERVICE"
  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "drop database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to drop database ${SERVICE}"
  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "create database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to re-create database ${SERVICE}"
# change ownership to the service user. If it fails we still can use restore but subsequent backups might fail on other datalakes.
  psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "GRANT ALL PRIVILEGES ON DATABASE ${SERVICE} TO ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || doLog "WARN: Unable to grant permissions to ${SERVICE}"
  psql --host="$HOST" --port="$PORT" --dbname="${SERVICE}" --username="$USERNAME" -c "ALTER SCHEMA public OWNER TO ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || doLog "WARN: Unable to change ownership to ${SERVICE}"

  if [ -f "$BACKUP_PLAIN" ]; then
      doLog "INFO restore a plain text dump file"
      psql --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" <"$BACKUP_PLAIN" >>$LOGFILE 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore ${SERVICE}"
  elif [ -f "$BACKUP_DUMP" ]; then
      doLog "INFO restore a custom format backup file"
      pg_restore --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="$SERVICE" "$BACKUP_DUMP" >>$LOGFILE 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore ${SERVICE}"
  fi
  doLog "INFO Successfully restored ${SERVICE}"
  limit_incomming_connection $SERVICE -1
}

run_restore() {
  mkdir -p "$BACKUPS_DIR"
  run_kinit

  if [[ $BACKUP_LOCATION == s3a://* ]] ;
  then
    STRIPPED_S3_URL=$(echo "$BACKUP_LOCATION" | sed -e 's/[a-fA-F0-9]\{8\}-[a-fA-F0-9]\{4\}-[a-fA-F0-9]\{4\}-[a-fA-F0-9]\{4\}-[a-fA-F0-9]\{12\}_database_backup\/*\*\?$//g')
    doLog "Begining to perform s3guard import for ${STRIPPED_S3_URL}"
    hadoop --loglevel ERROR s3guard import "$STRIPPED_S3_URL" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || doLog "ERROR Unable to import S3Guard metadata."
    doLog "s3guard import is complete"
  fi

  hdfs --loglevel ERROR dfs -copyToLocal -f "$BACKUP_LOCATION" "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Could not copy backups from ${BACKUP_LOCATION}."

  if [[ -z "$DATABASENAME" || "$DATABASENAME" == "DEFAULT" ]]; then
    echo "No database name provided. Will restore hive, ranger, profiler_agent, profiler_metric and knox_gateway databases."
    restore_db_from_local "hive"
    restore_db_from_local "ranger"
    restore_db_from_local "profiler_agent"
    restore_db_from_local "profiler_metric"
    restore_db_from_local "knox_gateway"
  else
    echo "Restoring ${DATABASENAME}."
    restore_db_from_local "${DATABASENAME}"
  fi

  rm -rfv "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

doLog "Info Ranger admin group is $RANGERGROUP"
doLog "INFO Initiating restore"
run_restore
doLog "INFO Completed restore."
