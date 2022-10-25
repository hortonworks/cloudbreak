#!/bin/bash

set -o nounset
set -o pipefail

# Logging configuration.
LOG_FILE=/var/log/get_datalake_solr_hbase_data_sizes.log
doLog() {
  if [ -n "${1-}" ]; then
    echo "$(date "+%Y-%m-%dT%H:%M:%S") $1" >>$LOG_FILE
  fi
}

errorExit() {
  doLog "Getting Solr/HBase data sizes failed due to: $1"
  exit 1
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

    # Get sizes of each table along with the name of the table. (Second field contains replicas so we ignore it)
    SIZES=($(hdfs --loglevel ERROR  dfs -du -x /hbase/data/default/ 2> >(doLog) | awk '{print $1" "$3}'))

    # Construct final output from response.
    RESULT=""
    TABLE_NAME=""
    CUR_SIZE=""
    IDX=0
    while [[ ${IDX} -lt ${#SIZES[@]} ]]; do
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

  # Obtain the hostnames of all nodes containing Solr data.
  SOLR_HOSTNAMES=$(curl -k -s --negotiate -u : "https://$(hostname -f):8985/solr/admin/collections?action=CLUSTERSTATUS" | \
    jq -c '.cluster.live_nodes[]')

  # Iterate through hostnames and build large JSON object consisting of a JSON object for each hostname with format:
  # {"collection1": ["shard1": <size>, "shard2": <size>]...}
  ALL_SOLR_SIZES=""
  CORRECTED_HOSTNAME=""
  CUR_RESULT=""

  for hostname in $SOLR_HOSTNAMES; do
    # Hostnames are of form: "<hostname>_solr".
    CORRECTED_HOSTNAME="https://$(echo ${hostname::${#hostname}-6} | tr -d '"')"
    CUR_RESULT=$(curl -k -s --negotiate -u : "${CORRECTED_HOSTNAME}/solr/admin/cores?action=STATUS&wt=json" | \
          jq -c '[.status[] | {"collection": .cloud.collection, "shard": .cloud.shard, "size": .index.sizeInBytes}]')
    ALL_SOLR_SIZES="${ALL_SOLR_SIZES}${CUR_RESULT},"
  done
  ALL_SOLR_SIZES=$(echo "[${ALL_SOLR_SIZES%?}]" | jq -c '[.[][]]')

  # Merge all sizes by removing any additional duplicates and adding shard sizes together.
  SIZES=$(echo ${ALL_SOLR_SIZES} | \
    jq -c 'reduce .[] as $x (null; .[$x.collection][$x.shard] = ([.[$x.collection][$x.shard], $x.size] | max)) |
    [to_entries[] | {(.key | tostring) : (.value | add)}] | add')
  doLog "Solr collection sizes result: ${SIZES}"

  doLog "Finished getting data sizes for Solr collections."
  echo "$SIZES"
}

outputDataSizes() {
  printf '"hbase":%s,"solr":%s\n' "$1" "$2"
}

# Empty/create log file.
>$LOG_FILE

# Perform main process.
doLog "Started getting HBase/Solr data sizes."
HBASE_DATA_SIZES=$(getDataSizesForHBase)
SOLR_DATA_SIZES=$(getDataSizesForSolr)
doLog "Finished getting HBase/Solr data sizes."

outputDataSizes "$HBASE_DATA_SIZES" "$SOLR_DATA_SIZES"
