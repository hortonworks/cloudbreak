#!/bin/bash -e

: ${LOGFILE:=/var/log/consul-watch/consul_handler.log}

: ${SOURCE_DIR:=/data/jars}
: ${STORAGE_JAR:=dash-azure-storage-2.2.0.jar}

main(){
  SOURCE_JAR="$SOURCE_DIR/$STORAGE_JAR"
  if [ ! -f "$SOURCE_JAR" ]; then
    echo 'DASH storage jar not found in the source directory, downloading it to /tmp.'
    SOURCE_JAR="/tmp/$STORAGE_JAR"
    curl -o "$SOURCE_JAR" "https://www.dash-update.net/client/Latest/StorageSDK2.0/$STORAGE_JAR";
  fi

  echo "Replacing azure-storage.jar with $STORAGE_JAR"
  find / -name "azure-storage*.jar" | while read line; do echo "Replacing $line"; \cp -f "$SOURCE_JAR" "${line%azure*}"; rm -f $line; done
}

exec &> "$LOGFILE"
[[ "$0" == "$BASH_SOURCE" ]] && main "$@"