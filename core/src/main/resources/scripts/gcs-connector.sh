#!/bin/bash -e

: ${LOGFILE:=/var/log/recipes/gcs-connector.log}

: ${STORAGE_JAR:=gcs-connector-latest-hadoop2.jar}
: ${TARGET_DIR:=/usr/lib/hadoop/lib/}

main(){
  SOURCE_JAR="/tmp/$STORAGE_JAR"
  if [ ! -f "$SOURCE_JAR" ]; then
    echo "Downloading GCS Connector for Hadoop"
    curl -Lko "$SOURCE_JAR" "https://storage.googleapis.com/hadoop-lib/gcs/gcs-connector-latest-hadoop2.jar"
    echo "Download finished"
  fi

  echo "Create target directory: $TARGET_DIR"
  mkdir -p $TARGET_DIR
  echo "Moving gcs-connector jar to $TARGET_DIR"
  mv "$SOURCE_JAR" "$TARGET_DIR"
}

exec &>> "$LOGFILE"
[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
