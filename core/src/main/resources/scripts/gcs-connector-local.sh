#!/bin/bash -e

: ${TARGET_DIR:=/usr/lib/hadoop/lib}
: ${STORAGE_JAR:=gcs-connector-latest-hadoop2.jar}

enable_gcs_connector_locally() {
  TARGET_JAR="$TARGET_DIR/$STORAGE_JAR"
  if [ ! -f "$TARGET_JAR" ]; then
    echo 'GC storage jar not found in the target directory, downloading it'
    mkdir -p $TARGET_DIR
    curl -Lko "$TARGET_JAR" "https://storage.googleapis.com/hadoop-lib/gcs/gcs-connector-latest-hadoop2.jar"
  fi
}

main(){
  if [ ! -f "/var/gcs-connector-enabled" ]; then
    enable_gcs_connector_locally
    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/gcs-connector-local-enabled
  else
    echo "The gcs-connector.sh has been executed previously."
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
