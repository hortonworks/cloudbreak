#!/bin/bash
# backup_db.sh
# This script uses the 'pg_dump' utility to dump the contents of hive and ranger PostgreSQL databases as plain SQL.
# After PostgreSQL contents are dumped, the SQL file is uploaded using the hdfs cli.
# For AWS or Azure, the url should start with `s3a://` or `abfs://`, respectively.

set -o nounset
set -o pipefail

LOGFILE=/var/log/dl_postgres_backup.log

echo "Logs at ${LOGFILE}"

exec 3>&1 4>&2
trap 'exec 2>&4 1>&3' 0 1 2 3
exec 1> >(tee -a "${LOGFILE}") 2> >(tee -a "${LOGFILE}" >&2)

doLog() {
  type_of_msg=$(echo "$@" | cut -d" " -f1)
  msg=$(echo "$*" | cut -d" " -f2-)
  [[ $type_of_msg == INFO ]] && type_of_msg="INFO " # one space for aligning
  [[ $type_of_msg == WARN ]] && type_of_msg="WARN " # as well

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

usage() {
  doLog "There might be missing values in /srv/pillar/postgresql/disaster_recovery.sls or /srv/pillar/postgresql/postgre.sls."
  doLog "This might be caused by the command not being run on the Primary Gateway node or due to never having run a backup/restore via the CDP CLI before."
  doLog "Script accepts the following inputs:"
  doLog "  -s path : Object Storage Service url to place backups."
  doLog "  -h host : PostgresSQL host name."
  doLog "  -p port : PostgresSQL port."
  doLog "  -u username : PostgresSQL user name."
  doLog "  -r group : Ranger admin group."
  doLog "  -c bool : Whether or not to close connections for the database while it is being backed up."
  doLog "  -z compression_level : (optional)  database dump compression level 0-9"
  doLog "  -d \"db1 db2 db3\" : (optional) Names of the databases to backup. If not given, will backup ranger and hive databases."
  doLog "  -l local_backup_dir : (optional) Local backup base directory. If not given, will use /var/tmp."
}

BACKUP_LOCATION=""
HOST=""
PORT=""
USERNAME=""
RANGERGROUP=""
CLOSECONNECTIONS=""
COMPRESSION=""
LOCAL_BACKUP_BASE_DIR=""

while getopts "s:h:p:u:r:c:z:d:l:" OPTION; do
    case $OPTION in
    s  )
        BACKUP_LOCATION="$OPTARG"
        ;;
    h  )
        HOST="$OPTARG"
        ;;
    p  )
        PORT="$OPTARG"
        ;;
    u  )
        USERNAME="$OPTARG"
        ;;
    r  )
        RANGERGROUP="$OPTARG"
        ;;
    c  )
        CLOSECONNECTIONS="$OPTARG"
        ;;
    z  )
        COMPRESSION=$OPTARG
        ;;
    l  )
        LOCAL_BACKUP_BASE_DIR="$OPTARG"
        ;;
    \? ) echo "Unknown option: -$OPTARG" >&2;
         usage
         exit 1;;
    :  ) echo "Missing option argument for -$OPTARG" >&2; exit 1;;
    esac
done

shift $((OPTIND-1))
DATABASENAMES="$@"


[ -z "$BACKUP_LOCATION" ]  || [[ $BACKUP_LOCATION == \-* ]] && doLog "backup location is not specified, use the -s option"
[ -z "$RANGERGROUP" ] || [[ $RANGERGROUP == \-* ]] && doLog "Ranger admin group is not specified, use the -r option"
[ -z "$CLOSECONNECTIONS" ] || [[ $CLOSECONNECTIONS == \-* ]] && doLog "Whether to close connections flag is not specified, use the -c option"

[ -z "$BACKUP_LOCATION" ] || [ -z "$RANGERGROUP" ] || [ -z "$CLOSECONNECTIONS" ] && doLog "At least one mandatory parameter is not set!" && usage && exit 1

doLog "Backup with compression level $COMPRESSION"

# Root directory for local postgres dump.
BACKUPS_DIR="${LOCAL_BACKUP_BASE_DIR:-/var/tmp}"

if ! validate_directory "$BACKUPS_DIR"; then
  doLog "ERROR Local backup directory is not valid"
  exit 1
fi

# Directory for the current postgres dump.
DATE_DIR=${BACKUPS_DIR}/$(date '+%Y-%m-%dT%H:%M:%SZ')

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE="{{ postgresql.ssl_verification_mode }}"
{%- endif %}

errorExit() {
  if [[ "$CLOSECONNECTIONS" == "true" ]]; then
    limit_incomming_connection $SERVICE -1
  fi
  if [ -d "$DATE_DIR" ] && [[ $DATE_DIR == ${BACKUPS_DIR}/* ]]; then
    rm -rf -v "$DATE_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
    doLog "Removed directory $DATE_DIR"
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
    errorExit "Unable to kinit as any principal! Something is wrong with the Kerberos connection. Please check logs for additional info."
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
  REMOTE_BACKUP_NAME="$2"
  run_kinit
  doLog "INFO Uploading to ${BACKUP_LOCATION}"

  hdfs --loglevel ERROR dfs -mkdir -p "$BACKUP_LOCATION"
  OBJECT_STORE_PATH="${BACKUP_LOCATION}/${REMOTE_BACKUP_NAME}"
  hdfs --loglevel ERROR dfs -moveFromLocal -f "$LOCAL_BACKUP" "$OBJECT_STORE_PATH" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to upload $SERVICE backup"
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
  doLog "INFO limit existing connections to ${COUNT} on ${SERVICE}"
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
    doLog "WARN database for $SERVICE doesn't exist"
    return 0
  fi

  if [[ "$CLOSECONNECTIONS" == "true" ]]; then
    limit_incomming_connection $SERVICE 0
    close_existing_connections $SERVICE
  fi

  doLog "INFO Dumping ${SERVICE}"
  LOCAL_BACKUP=${DATE_DIR}/${SERVICE}_backup
  if [[ "$SERVICE" == "ranger" ]]; then
    REMOTE_BACKUP_NAME="ranger_backup"
    doLog "INFO Do not compress the 'ranger' database"
    pg_dump --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="$SERVICE" --format=plain --file="$LOCAL_BACKUP" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump ${SERVICE}"
    replace_ranger_group_before_export $RANGERGROUP $LOCAL_BACKUP
  elif [[ ${COMPRESSION} -eq 0 ]] || [[ -z ${COMPRESSION} ]]; then
    REMOTE_BACKUP_NAME="${SERVICE}_backup"
    doLog "INFO Do not compress database ${SERVICE}, plain SQL output"
    pg_dump --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="$SERVICE" --format=plain --file="$LOCAL_BACKUP" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump ${SERVICE}"
  else
    doLog "INFO Use compressed dump for the ${SERVICE} database"
    REMOTE_BACKUP_NAME="${SERVICE}_backup.dump"
    pg_dump --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname="$SERVICE" --format=custom --file="$LOCAL_BACKUP" -Z $COMPRESSION > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump ${SERVICE}"
  fi

  if [[ "$CLOSECONNECTIONS" == "true" ]]; then
    limit_incomming_connection $SERVICE -1
  fi

  BACKUP_SIZE=$(du -h $LOCAL_BACKUP | cut -f 1)
  doLog "INFO ${SERVICE} backup size is ${BACKUP_SIZE}"

  move_backup_to_cloud "$LOCAL_BACKUP" "$REMOTE_BACKUP_NAME"

  doLog "INFO Completed upload to ${BACKUP_LOCATION}"
  doLog "INFO ${SERVICE} dumped to ${OBJECT_STORE_PATH}"
}

run_backup() {
  mkdir -p "$DATE_DIR" || error_exit "Could not create local directory for backups."

  doLog "INFO Conditional variable for closing connections to database during backup is set to ${CLOSECONNECTIONS}"

  if [[ -z "$DATABASENAMES" || "$DATABASENAMES" == "DEFAULT" ]]; then
    doLog "INFO No database name provided. Will backup hive, ranger, profiler_agent and profiler_metric databases."
    backup_database_for_service "hive"
    backup_database_for_service "ranger"
    backup_database_for_service "profiler_agent"
    backup_database_for_service "profiler_metric"
  else
    for db in $DATABASENAMES; do
      doLog "INFO Backing up ${db}."
      backup_database_for_service "${db}"
    done
  fi

  rmdir -v "$DATE_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
}

doLog "INFO Starting backup to ${BACKUP_LOCATION}"
run_backup
doLog "INFO Completed backup ${BACKUP_LOCATION}"
