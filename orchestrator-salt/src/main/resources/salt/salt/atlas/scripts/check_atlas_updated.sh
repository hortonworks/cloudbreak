#!/bin/bash

# Files used by the script.
LOGFILE=/var/log/check_atlas_updated.log
JAASFILE=/var/log/jaas.conf
CONFIGFILE=/var/log/client.config

doLog() {
  echo "$(date "+%Y-%m-%dT%H:%M:%SZ") $1" >>$LOGFILE
}

initFiles() {
  # Setup required configuration files.
  >$JAASFILE
  printf "KafkaClient {
  \tcom.sun.security.auth.module.Krb5LoginModule required
  \tuseKeyTab=true
  \tkeyTab=\"%s\"
  \tprincipal=\"%s\";\n};\n" "${ATLAS_KT}" "${ATLAS_PRINCIPAL}" > "$JAASFILE"

  >$CONFIGFILE
  printf "security.protocol=SASL_SSL\nsasl.kerberos.service.name=kafka\n" > "$CONFIGFILE"

  doLog "Finished setting up config files: ${JAASFILE} and ${CONFIGFILE}."
}

cleanupFiles() {
  rm -f "$JAASFILE"
  rm -f "$CONFIGFILE"
}

check_atlas_lineage() {
  # Obtain Atlas lineage information.
  LINEAGE_INFO=$(/opt/cloudera/parcels/CDH/lib/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server "${KAFKA_SERVER}" --describe --group atlas \
    --command-config="${CONFIGFILE}" 2>/dev/null \
    | awk '{print $2, $6}')

  if [[ -z "$LINEAGE_INFO" ]]; then
    doLog "*ERROR*: Unable to get lineage info for Atlas. Please look at the created configuration files to make sure they look correct."
    return 2
  fi

  # Parse lineage information and determine if Atlas is out of date.
  LINEAGE_LAG_VALS=($LINEAGE_INFO)
  NUM_LAG_VALS=${#LINEAGE_LAG_VALS[@]}
  OUT_OF_DATE_TOPICS=""
  for (( i = 2; i < ${NUM_LAG_VALS}; i += 2 )); do
    if [[ ${LINEAGE_LAG_VALS[${i} + 1]} != '-' && ${LINEAGE_LAG_VALS[${i} + 1]} != '0' ]]; then
      OUT_OF_DATE_TOPICS="${OUT_OF_DATE_TOPICS}${LINEAGE_LAG_VALS[$i]}, "
    fi
  done

  if [[ -z "$OUT_OF_DATE_TOPICS" ]]; then
    doLog "Atlas is now up to date."
    return 0
  else
    doLog "The following Atlas topics are not up to date: ${OUT_OF_DATE_TOPICS%??}."
    return 1
  fi
}

# Empty/create log file.
>$LOGFILE

# Setup necessary variables for Kafka and the main Atlas check.
KAFKA_SERVER=$(grep --line-buffered -oP "atlas.kafka.bootstrap.servers=\K.*" \
  /etc/atlas/conf/atlas-application.properties | awk -F',' '{print $1}')
doLog "Using Kafka bootstrap server: ${KAFKA_SERVER}."

ATLAS_KT=$(find / -wholename "*atlas-ATLAS_SERVER/atlas.keytab" 2>/dev/null | head -n 1)
doLog "Using Atlas keytab: ${ATLAS_KT}."

ATLAS_PRINCIPAL=$(klist -kt "${ATLAS_KT}" | grep -o -m 1 "atlas\/\S*")
doLog "Using Atlas principal: ${ATLAS_PRINCIPAL}."

export KAFKA_HEAP_OPTS="-Xms512m -Xmx1g"
export KAFKA_OPTS="-Djava.security.auth.login.config=${JAASFILE}"

# File setup.
$(initFiles)

# Kinit into Atlas before starting check process.
kinit -kt "$ATLAS_KT" "atlas/$(hostname -f)" 2>/dev/null
doLog "Finished kinit-ing into Atlas keytab."

# Iteratively check Atlas until it is up to date.
MAX_RETRIES=$1
doLog "Starting the waiting process for Atlas being fully up to date. Will retry a max of ${MAX_RETRIES} times."
RESPONSE=1
j=0
while : ; do
  $(check_atlas_lineage)
  RESPONSE=$?

  if [[ "$RESPONSE" == "0" ]]; then
    break
  elif [[ "$RESPONSE" == "2" ]]; then
    RESPONSE=1
    break
  fi

  ((j++))
  if [[ "$j" == "$MAX_RETRIES" ]]; then
    doLog "Timed out while waiting for Atlas to be up to date."
    break
  fi

  doLog "Waiting some more for Atlas to be entirely up to date."
  sleep 5
done

# Done.
doLog "Spent $((j * 5)) seconds waiting for Atlas to be up to date."
$(cleanupFiles)
exit $RESPONSE
