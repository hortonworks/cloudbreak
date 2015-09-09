#!/bin/bash -e

: ${LOGFILE:=/var/log/consul-watch/consul_handler.log}

: ${SOURCE_DIR:=/data/jars}
: ${STORAGE_JAR:=dash-azure-storage-2.2.0.jar}
: ${MR_TAR_NAME:=mapreduce.tar.gz}

main(){
  SOURCE_JAR="$SOURCE_DIR/$STORAGE_JAR"
  if [ ! -f "$SOURCE_JAR" ]; then
    echo 'DASH storage jar not found in the source directory, downloading it to /tmp.'
    SOURCE_JAR="/tmp/$STORAGE_JAR"
    curl "https://www.dash-update.net/client/Latest/StorageSDK2.0/$STORAGE_JAR" > "$SOURCE_JAR";
  fi

  echo "Finding $MR_TAR_NAME on HDFS"
  MR_TAR_PATH="$(hadoop fs -ls /hdp/apps | sed '1d;s/  */ /g' | cut -d\  -f8)/mapreduce/$MR_TAR_NAME"

  echo "Copying $MR_TAR_NAME from HDFS to local fs."
  [[ -f "/tmp/$MR_TAR_NAME" ]] && rm "/tmp/$MR_TAR_NAME"
  hadoop fs -copyToLocal "$MR_TAR_PATH" /tmp

  echo "Extracting $MR_TAR_NAME."
  cd /tmp && tar -zxf $MR_TAR_NAME

  echo "Replacing azure-storage.jar with $STORAGE_JAR."
  find hadoop/ -name "azure-storage*.jar" | while read line; do echo Replace $line; \cp -f $SOURCE_JAR ${line%azure*}; rm -f $line; done

  echo "Removing $MR_TAR_NAME."
  rm -f "$MR_TAR_NAME"

  echo "Creating new $MR_TAR_NAME with the replaced libs."
  tar -zcf "$MR_TAR_NAME" hadoop

  echo "Replacing $MR_TAR_NAME on HDFS."
  su -c "hadoop fs -put -f $MR_TAR_NAME $MR_TAR_PATH" hdfs

  echo "Cleaning up local fs."
  rm -f "$MR_TAR_NAME"
  rm -rf hadoop
}

exec &> "$LOGFILE"
[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
