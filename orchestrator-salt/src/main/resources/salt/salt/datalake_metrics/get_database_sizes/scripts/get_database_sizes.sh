#!/bin/bash

set -o nounset
set -o pipefail

# Logging configuration.
LOG_FILE=/var/log/get_datalake_database_sizes.log
doLog() {
  if [ -n "${1-}" ]; then
    echo "$(date "+%Y-%m-%dT%H:%M:%S") $1" >>$LOG_FILE
  fi
}

# Import validation.
if [[ $# -lt 3 ]]; then
  doLog "Invalid inputs provided"
  doLog "Script requires the following inputs:"
  doLog "  1. PostgreSQL host name."
  doLog "  2. PostgreSQL port."
  doLog "  3. PostgreSQL user name."
  exit 1
fi

# Global variables for the script.
PSQL_HOST="$1"
PSQL_PORT="$2"
PSQL_USERNAME="$3"

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
  DATABASES='"hive" "ranger" "profiler_agent" "profiler_metric"'
  RESULT=""
  CUR_DB_SIZE=0
  TRIMMED_DB=""
  for DB in $DATABASES; do
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
