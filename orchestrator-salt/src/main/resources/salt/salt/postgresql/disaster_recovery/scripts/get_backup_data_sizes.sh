#!/bin/bash

set -o nounset
set -o pipefail

# Import validation.
if [[ $# -lt 3 ]]; then
  echo "Invalid inputs provided"
  echo "Script requires the following inputs:"
  echo "  1. PostgreSQL host name."
  echo "  2. PostgreSQL port."
  echo "  3. PostgreSQL user name."
  exit 1
fi

# Global variables for the script.
PSQL_HOST="$1"
PSQL_PORT="$2"
PSQL_USERNAME="$3"
LOG_FILE=/var/log/get_backup_data_sizes.log

# PSQL SSL configuration.
{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

doLog() {
  if [ -n "${1-}" ]; then
    echo "$(date "+%Y-%m-%dT%H:%M:%S") $1" >>$LOG_FILE
  fi
}

errorExit() {
  doLog "Getting backup data sizes failed due to: $1"
  exit 1
}

runPSQLCommand() {
  CMD=$1
  echo $(psql --host="$PSQL_HOST" --port="$PSQL_PORT" --dbname="postgres" \
    --username="$PSQL_USERNAME" -c "$CMD" -At 2> >(doLog))
}

doesDatabaseExist() {
  DESIRED_DB=$1
  doLog "Checking the existence of database ${DESIRED_DB}"
  FOUND_DB=$(runPSQLCommand "SELECT datname FROM pg_catalog.pg_database WHERE datname = '${DESIRED_DB}';")
  if [[ "$FOUND_DB" != "$DESIRED_DB" ]]; then
    doLog "Database ${DESIRED_DB} does not exist! Skipping it for this process."
    return 1
  fi
  return 0
}

getDataSizesForDatabases() {
  doLog "Getting the data sizes for the local databases."

  DATABASES='"hive" "ranger" "profiler_agent" "profiler_metric"'
  RESULT=""

  CUR_DB_SIZE=0
  TRIMMED_DB=""
  for DB in $DATABASES; do
    TRIMMED_DB=$(echo "$DB" | tr -d '"')
    if doesDatabaseExist "$TRIMMED_DB" ; then
      CUR_DB_SIZE=$(runPSQLCommand "SELECT pg_database_size('${TRIMMED_DB}');")
      RESULT="${RESULT}${DB}:${CUR_DB_SIZE},"
      doLog "Size of database ${DB} is ${CUR_DB_SIZE} bytes."
    fi
  done

  doLog "Finished getting the data sizes for the local databases."
  echo "{${RESULT%?}}"
}

kinit_as() {
  doLog "Attempting kinit as $1 using Keytab: $2"
  kinit -kt "$2" "$1/$(hostname -f)"
  if [[ $? -ne 0 ]]; then
    errorExit "Couldn't kinit as $1."
  fi
}

getDataSizesForHBase() {
    doLog "Getting data sizes for HBase tables."

    HBASE_KEYTAB=$(find /run/cloudera-scm-agent/process/ \
      -name "*.keytab" -a \( -path "*hbase-REGIONSERVER*" -o -path "*hbase-MASTER*" \) \
      -a -type f | head -n 1)
    kinit_as "hbase" "$HBASE_KEYTAB"

    # Get sizes of each table along with the name of the table. (Sizes are duplicated which is why we skip the second field)
    SIZES=($(hdfs --loglevel ERROR  dfs -du -x /hbase/data/default/ 2> >(doLog) | awk '{print $1" "$3}'))

    # Construct final output from response.
    RESULT=""
    TABLE_NAME=""
    CUR_SIZE=""
    IDX=0
    {% raw %}
    while [[ ${IDX} -lt ${#SIZES[@]} ]]; do
    {% endraw %}
      CUR_SIZE="${SIZES[$IDX]}"
      TABLE_NAME="${SIZES[$((IDX+1))]##*/}" # Remove preceding directories in path to table.
      TABLE_NAME=$(echo ${TABLE_NAME} | awk '{print tolower($0)}')
      doLog "Size of HBase table \"${TABLE_NAME}\" is ${CUR_SIZE} bytes."

      RESULT="${RESULT}\"${TABLE_NAME}\":${CUR_SIZE},"
      IDX=$((IDX+2))
    done

    doLog "Finished getting data sizes for HBase tables."
    echo "{${RESULT%?}}"
}

getDataSizesForSolr() {
  doLog "Getting data sizes for Solr collections."

  SOLR_KEYTAB=$(find /run/cloudera-scm-agent/process/ -name "*.keytab" -path "*solr-SOLR_SERVER*" | head -n 1)
  kinit_as "solr" "$SOLR_KEYTAB"

  # Obtain sizes for all Solr shards. Below query simply obtains the sizes of each shard for each collection, ensuring to ignore replicas by taking
  # the maximum size replica, and then summing the sizes of the shards for each collection.
  SIZES=$(curl -k -s --negotiate -u : "https://$(hostname -f):8985/solr/admin/cores?action=STATUS&wt=json&indent=true" | \
    jq -c 'reduce .status[] as $x (null; .[$x.cloud.collection][$x.cloud.shard] = ([.[$x.cloud.collection][$x.cloud.shard], $x.index.sizeInBytes] | max))
    | reduce to_entries[] as $x (null; .[$x.key] += ([$x.value[]] | add))')
  doLog "Solr collection sizes result: ${SIZES}"

  doLog "Finished getting data sizes for Solr collections."
  echo "$SIZES"
}

outputDataSizes() {
  printf '{"database":%s,"hbase":%s,"solr":%s}\n' "$1" "$2" "$3"
}

# Empty/create log file.
>$LOG_FILE

# Perform main process.
doLog "Starting process for getting backup data sizes."
DB_DATA_SIZES=$(getDataSizesForDatabases)
HBASE_DATA_SIZES=$(getDataSizesForHBase)
SOLR_DATA_SIZES=$(getDataSizesForSolr)
doLog "Finished process for getting backup data sizes."
outputDataSizes "$DB_DATA_SIZES" "$HBASE_DATA_SIZES" "$SOLR_DATA_SIZES"
