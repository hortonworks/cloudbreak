#!/bin/bash
# dry-run.sh
# This script try to imitates the 'backup_db.sh' operations, also it try to cover the cloud-provider client action which executed
# by independent services like 'Solr', 'HBase'

set -o nounset
set -o pipefail

LOGFILE=/var/log/dl_postgres_dry_run_restore.log

echo "Logs are at ${LOGFILE}"

doLog() {
  type_of_msg=$(echo "$@" | cut -d" " -f1)
  msg=$(echo "$*" | cut -d" " -f2-)
  [[ $type_of_msg == INFO ]] && type_of_msg="INFO   " # three space for aligning
  [[ $type_of_msg == WARN ]] && type_of_msg="WARN   " # three space for aligning
  [[ $type_of_msg == ERROR ]] && type_of_msg="ERROR " # one space for aligning
  test -t 1 && echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg"
  if [[ $type_of_msg == "ERROR " ]]; then
    echo >&2 "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg"
  else
    echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $type_of_msg ""$msg" >>$LOGFILE
  fi
}

usage() {
  doLog "INFO Script accepts the following inputs:"
  doLog "INFO   -s path : Object Storage Service url to place backups."
  doLog "INFO   -h host : PostgreSQL host name."
  doLog "INFO   -p port : PostgreSQL port."
  doLog "INFO   -u username : PostgreSQL user name."
  doLog "INFO   -d \"db1 db2 db3\" : Database names (space-separated, at least one required)."
  doLog "INFO   -r raz_enabled : RAZ enabled flag."
  doLog "INFO   -l local_dir : Local backup directory (optional, defaults to /var/tmp)."
}

BACKUP_LOCATION=""
HOST=""
PORT=""
USERNAME=""
DATABASENAMES=""
RAZ_ENABLED=""
LOCAL_BACKUP_BASE_DIR="/var/tmp"
SERVICE=""
FAILED=0

while getopts "s:h:p:u:d:r:l:" OPTION; do
    case $OPTION in
    s  )
        BACKUP_LOCATION="$OPTARG/*"  # Trailing slash and glob so we copy the _items_ in the directory not the directory itself.
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
    d  )
        DATABASENAMES="$OPTARG"
        ;;
    r  )
        RAZ_ENABLED="$OPTARG"
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

[ -z "$BACKUP_LOCATION" ]  || [[ $BACKUP_LOCATION == \-* ]] && doLog "INFO Object storage URL is not specified, use the -s option"
[ -z "$DATABASENAMES" ] || [[ $DATABASENAMES == \-* ]] && doLog "INFO Database names are not specified, use the -d option"

[ -z "$BACKUP_LOCATION" ] || [ -z "$DATABASENAMES" ] && doLog "ERROR At least one mandatory parameter is not set!" && usage && exit 1
# Script appends its own suffix
BACKUPS_DIR="${LOCAL_BACKUP_BASE_DIR}/postgres_dry_run_restore"

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE="{{ postgresql.ssl_verification_mode }}"
{%- endif %}

errorExit() {
  if [ -d "$BACKUPS_DIR" ] && [[ $BACKUPS_DIR == */postgres_dry_run_restore* || $BACKUPS_DIR == */postgres_restore_staging* ]]; then
    rm -rfv "$BACKUPS_DIR" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
    doLog "INFO Removed directory $BACKUPS_DIR"
  fi
  doLog "ERROR $1"
  exit 1

}

kinit_as() {
  doLog "INFO attempting kinit as $1 using Keytab: $2"
  kinit -kt "$2" "$1/$(hostname -f)"
  # shellcheck disable=SC2181
  if [[ $? -ne 0 ]]; then
    doLog "ERROR Couldn't kinit as $1"
    return 1
  fi
}

is_database_exists() {
  SERVICE=$1
  doLog "INFO Checking the existence of database ${SERVICE}"
  database_name=$(psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "SELECT datname FROM pg_catalog.pg_database WHERE datname = '$SERVICE';" -At > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) ||
    doLog "ERROR failed ot check database existence")
  if [[ "$database_name" != "$SERVICE" ]]; then
    return 1
  fi
  return 0
}

make_dir() {
  if ! hdfs --loglevel FATAL dfs -mkdir -p "${OBJECT_STORE_PATH}" >>${LOGFILE} 2>&1; then
    doLog $1
    ((FAILED++))
  fi
}

move_from_local() {
  if ! hdfs --loglevel FATAL dfs -moveFromLocal -f "$LOCAL_TESTFILE_LOCATION" "$REMOTE_TESTFILE_LOCATION" >>${LOGFILE} 2>&1; then
    doLog $1
    ((FAILED++))
  fi
}

copy_to_local() {
  if ! hdfs --loglevel FATAL dfs -copyToLocal -f "$REMOTE_TESTFILE_LOCATION" "$LOCAL_RESTORE_TESTFILE_LOCATION" >>${LOGFILE} 2>&1; then
    doLog $1
    ((FAILED++))
  fi
}

execute_run() {
  doLog "INFO Validate Database connection, and the database existence"
  doLog "INFO Checking databases: ${DATABASENAMES}"

  for DB in $DATABASENAMES; do
    TRIMMED_DB=$(echo "$DB" | tr -d '"')
    is_database_exists "$TRIMMED_DB"
    if [ "$?" -eq 1 ]; then
      doLog "ERROR database $TRIMMED_DB doesn't exist"
      ((FAILED++))
    else
      doLog "INFO database $TRIMMED_DB exists"
    fi
  done

  doLog "INFO backup dir:" "$BACKUPS_DIR"
  mkdir -p "$BACKUPS_DIR"
  OBJECT_STORE_PATH=${BACKUP_LOCATION::-1}
  LOCAL_TESTFILE_LOCATION="${BACKUPS_DIR}"/dry-run.txt
  REMOTE_TESTFILE_LOCATION="${OBJECT_STORE_PATH}"dry-run.txt
  LOCAL_RESTORE_TESTFILE_LOCATION="${BACKUPS_DIR}"/dry-run-restore.txt
  touch "${LOCAL_TESTFILE_LOCATION}"
  doLog "INFO Object storage path" ${OBJECT_STORE_PATH}
  doLog "INFO testFile backup location" ${LOCAL_TESTFILE_LOCATION}
  doLog "INFO testFile remote location" ${REMOTE_TESTFILE_LOCATION}
  doLog "INFO testFile local restore location" ${LOCAL_RESTORE_TESTFILE_LOCATION}

  export KRB5CCNAME=/tmp/krb5cc_cloudbreak_$EUID

  ATLAS_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -path "*atlas-ATLAS_SERVER*" | head -n 1)
  HDFS_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -path "*hdfs*" | head -n 1)

  ATLAS_KEYTAB
  if [[ $RAZ_ENABLED ]]; then
    kinit_as atlas "$ATLAS_KEYTAB"
    doLog "INFO Try moveFromLocal via HDFS"
    make_dir "ERROR Failed to make directory on the backup location please check the permissions on the backup location for the Ranger Raz Role"
    move_from_local "ERROR Failed to moveFromLocal from the backup location please check the permissions on the backup location for the Ranger Raz Role"
    doLog "INFO Try copyToLocal via HDFS"
    copy_to_local "ERROR Failed to copyToLocal from the backup location please check the permissions on the backup location for the Ranger Raz Role"
  else
    ## IDBroker
    kinit_as atlas "$ATLAS_KEYTAB"
    doLog "INFO Try moveFromLocal via HDFS"
    make_dir "ERROR Failed to make directory on the backup location please check the permissions on the backup location for the Ranger Audit Role"
    move_from_local "ERROR Failed to moveFromLocal from the backup location please check the permissions on the backup location for the Ranger Audit Role"
    doLog "INFO Try copyToLocal via HDFS"
    copy_to_local "ERROR Failed to copyToLocal from the backup location please check the permissions on the backup location for the Ranger Audit Role"

    kinit_as hdfs "$HDFS_KEYTAB"
    doLog "INFO Try copyToLocal via HDFS"
    make_dir "ERROR Failed to make directory on the backup location please check the permissions on the backup location for the Datalake Admin Role"
    move_from_local "ERROR Failed to moveFromLocal from the backup location please check the permissions on the backup location for the Datalake Admin Role"
    doLog "INFO Try copyToLocal via HDFS"
    copy_to_local "ERROR Failed to copyToLocal from the backup location please check the permissions on the backup location for the Datalake Admin Role"
  fi
  rm -rfv "$BACKUPS_DIR"
}

doLog "Checking Validation for directory: $LOCAL_BACKUP_BASE_DIR"

# Check if directory exists
if [[ ! -d "$LOCAL_BACKUP_BASE_DIR" ]]; then
  doLog "ERROR: Directory '$LOCAL_BACKUP_BASE_DIR' does not exist"
  ((FAILED++))
fi

# Check if directory is readable and writable
if [[ ! -r "$LOCAL_BACKUP_BASE_DIR" || ! -w "$LOCAL_BACKUP_BASE_DIR" ]]; then
  doLog "ERROR: Directory '$LOCAL_BACKUP_BASE_DIR' is not readable or writable"
  ((FAILED++))
fi

doLog "INFO Initiating dry-run"
execute_run
if [ "$FAILED" -gt 0 ]; then
  doLog "ERROR The dry-run validation failed"
  exit 1
fi
doLog "INFO Completed dry-run."