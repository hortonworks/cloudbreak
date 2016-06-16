#!/bin/bash -e

: ${LOGFILE:=/var/log/recipes/gcs-p12.log}
: ${TARGET_DIR:=/usr/lib/hadoop/lib}

main(){
  mkdir -p $TARGET_DIR
  echo $P12KEY | base64 -d > $TARGET_DIR/gcp.p12
  echo "p12 file successfully saved to $TARGET_DIR/gcp.p12"
}

exec &>> "$LOGFILE"
[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
