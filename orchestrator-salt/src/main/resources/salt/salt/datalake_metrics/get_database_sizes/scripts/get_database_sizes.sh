#!/bin/bash

set -o nounset
set -o pipefail

LOG_FILE=/var/log/get_datalake_database_sizes.log
doLog() {
  if [ -n "${1-}" ]; then
    echo "$(date "+%Y-%m-%dT%H:%M:%S") $1" >>$LOG_FILE
  fi
}

usage() {
  doLog "Script accepts the following inputs:"
  doLog "  -h host : PostgreSQL host name."
  doLog "  -p port : PostgreSQL port."
  doLog "  -u username : PostgreSQL user name."
  doLog "  -d \"db1 db2 db3\" : Database names (space-separated, at least one required)."
}

PSQL_HOST=""
PSQL_PORT=""
PSQL_USERNAME=""
DATABASENAMES=""

while getopts "h:p:u:d:" OPTION; do
    case $OPTION in
    h  )
        PSQL_HOST="$OPTARG"
        ;;
    p  )
        PSQL_PORT="$OPTARG"
        ;;
    u  )
        PSQL_USERNAME="$OPTARG"
        ;;
    d  )
        DATABASENAMES="$OPTARG"
        ;;
    \? ) echo "Unknown option: -$OPTARG" >&2;
         usage
         exit 1;;
    :  ) echo "Missing option argument for -$OPTARG" >&2; exit 1;;
    esac
done

[ -z "$DATABASENAMES" ] || [[ $DATABASENAMES == \-* ]] && doLog "Database names are not specified, use the -d option"

[ -z "$DATABASENAMES" ] && doLog "At least one mandatory parameter is not set!" && usage && exit 1

# PSQL SSL configuration.
export PGSSLMODE=require

errorExit() {
  doLog "Getting database sizes failed due to: $1"
  exit 1
}

runPSQLCommand() {
  CMD=$1
  DB=$2
  psql --host="$PSQL_HOST" --port="$PSQL_PORT" --dbname="$DB" \
    --username="$PSQL_USERNAME" -c "$CMD" -At 2> >(doLog)
}

doesDatabaseExist() {
  DESIRED_DB=$1
  doLog "Checking the existence of database ${DESIRED_DB}"
  FOUND_DB=$(runPSQLCommand "SELECT datname FROM pg_catalog.pg_database WHERE datname = '${DESIRED_DB}';" "postgres")
  if [[ "$FOUND_DB" != "$DESIRED_DB" ]]; then
    doLog "Database ${DESIRED_DB} does not exist! Skipping it for this process."
    return 1
  fi
  return 0
}

getDataSizesForDatabases() {
  doLog "Processing databases: ${DATABASENAMES}"
  RESULT=""
  CUR_DB_SIZE=0
  TRIMMED_DB=""
  for DB in $DATABASENAMES; do
    TRIMMED_DB=$(echo "$DB" | tr -d '"')
    if doesDatabaseExist "$TRIMMED_DB" ; then
      CUR_DB_SIZE=$(runPSQLCommand "select sum(pg_table_size(quote_ident(tablename)::regclass)) from pg_tables where schemaname not in ('pg_catalog','information_schema');" "$TRIMMED_DB")
      if [ -z "$CUR_DB_SIZE" ]; then
        doLog "Unable to get size of database ${DB}, trying to use an alternative method"
        CUR_DB_SIZE=$(runPSQLCommand "SELECT pg_database_size('${TRIMMED_DB}');" "postgres")
      fi
      if [ -z "$CUR_DB_SIZE" ]; then
        doLog "Unable to get size of database ${DB} even though it seems to exist..."
      else
        RESULT="${RESULT}${DB}:${CUR_DB_SIZE},"
        doLog "Size of database ${DB} is ${CUR_DB_SIZE} bytes."
      fi
    fi
  done

  echo "{${RESULT%?}}"
}

outputDataSizes() {
  printf '"database":%s\n' "$1"
}

# Empty/create log file.
>$LOG_FILE

# Perform main process.
doLog "Started getting the data sizes for the local databases."
DB_DATA_SIZES=$(getDataSizesForDatabases)
doLog "Finished getting the data sizes for the local databases."

outputDataSizes "$DB_DATA_SIZES"
