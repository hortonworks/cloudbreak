#!/bin/bash
# restore_db.sh
# This script uses the 'psql' cli to drop hive and ranger databases, create them, then read in a plain SQL file to restore data.
# We retrieve the SQL files from a valid URL, which should be `s3a://` or `abfs://` for AWS or Azure clouds respectively.

set -o nounset
set -o pipefail

LOGFILE=/var/log/dl_postgres_restore.log
echo "Logs at ${LOGFILE}"

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

log_invalid_input_reason() {
  doLog "This might be caused by the command not being run on the Primary Gateway node or due to never having run a backup/restore via the CDP CLI before."
}

# Print out all required inputs if they are passed in.
while getopts ":s:h:p:u:g:n:" OPTION; do
    case $OPTION in
    s  )
        STORAGE_URL="$OPTARG"
        doLog "Object Storage Service url to retrieve backups: $STORAGE_URL"
        ;;
    h  )
        HOST="$OPTARG"
        doLog "PostgreSQL host name: $HOST"
        ;;
    p  )
        PORT="$OPTARG"
        doLog "PostgreSQL port: $PORT"
        ;;
    u  )
        USERNAME="$OPTARG"
        doLog "PostgreSQL user name: $USERNAME"
        ;;
    g  )
        GROUP="$OPTARG"
        doLog "Ranger admin group: $GROUP"
        ;;
    n  )
        DB_NAME+=("$OPTARG")
        DB_NAME_ITEM="$OPTARG"
        doLog "Names of the databases to restore."
        ;;
    \? ) doLog "Unknown option: -$OPTARG"
        exit 1;;
    :  ) doLog "Missing option argument for -$OPTARG"
        log_invalid_input_reason
        exit 1;;
    esac
done

# If any of the required inputs are missing or the provided values are NULL, the script can not run successfully.
[ -z ${STORAGE_URL+x} ] || [ -z ${GROUP+x} ] || [ -z "$STORAGE_URL" ] || [ -z "$GROUP" ] &&
doLog "At least one mandatory parameter is not set in disaster_recovery.sls: Object Storage Service URL; Ranger admin group." && log_invalid_input_reason && exit 1
[ -z ${HOST+x} ] || [ -z ${PORT+x}  ] || [ -z ${USERNAME+x} ] ||
[ -z "$HOST" ] || [ -z "$PORT" ] || [ -z "$USERNAME" ] &&
doLog "At least one mandatory parameter is not set in postgres: PostgresSQL host name, PostgresSQL port, PostgresSQL user name." && log_invalid_input_reason && exit 1

BACKUP_LOCATION="$STORAGE_URL/*" # Trailing slash and glob so we copy the _items_ in the directory not the directory itself.
HOST="$HOST"
PORT="$PORT"
USERNAME="$USERNAME"
RANGERGROUP="$GROUP"
# Assign values to db name based on if var DB_NAME has been set and if the value set to the var is null.
if [ -z ${DB_NAME+x} ] || [ -z "$DB_NAME" ]; then
  doLog "No database names are provided in disaster_recovery.sls. Will restore ranger and hive by default."
  DATABASENAME=""
else
  DATABASENAME="${DB_NAME[@]}"
fi

BACKUPS_DIR="/var/tmp/postgres_restore_staging"

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

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
  BACKUP="${BACKUPS_DIR}/${SERVICE}_backup"

  is_database_exists $SERVICE
  if [ "$?" -eq 1 ];then
    doLog "WARN database for $SERVICE doesn't exist"
    return 0
  fi

  if [[ "$SERVICE" == "ranger" ]]; then
    replace_ranger_group_before_import $RANGERGROUP $BACKUP
  fi
  if [ -f "$BACKUP" ]; then
    limit_incomming_connection $SERVICE 0
    close_existing_connections $SERVICE
    doLog "INFO Restoring $SERVICE"
    psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "drop database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to drop database ${SERVICE}"
    psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "create database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to re-create database ${SERVICE}"
    psql --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" <"$BACKUP" >$LOGFILE 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore ${SERVICE}"
    doLog "INFO Successfully restored ${SERVICE}"
    limit_incomming_connection $SERVICE -1
else
    doLog "INFO Not restoring ${SERVICE} as ${BACKUP} does not exist"
fi
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

  if [[ -z "$DATABASENAME" ]]; then
    echo "No database name provided. Will restore hive, ranger, profiler_agent and profiler_metric databases."
    restore_db_from_local "hive"
    restore_db_from_local "ranger"
    restore_db_from_local "profiler_agent"
    restore_db_from_local "profiler_metric"
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
