#!/bin/bash

set -e

CERT_ROOT_PATH=/etc/certs
CERT_BACKUP_PATH=/etc/certs-backup
CERT_TEMP_PATH=/etc/certs-new-temp

clear-temp-folders() {
  echo "Cleanup rotation temp folders."
  rm -rf $CERT_BACKUP_PATH
  rm -rf $CERT_TEMP_PATH
}

replace-jumpgate-key() {
  echo "Replacing jumpgate key."
  cp $CERT_ROOT_PATH/cluster.pem /etc/jumpgate/cluster.pem
  chmod 600 /etc/jumpgate/cluster.pem
  chown jumpgate:jumpgate /etc/jumpgate/cluster.pem
}

backup-current-cert-files() {
  echo "Backing up current certs."
  mkdir $CERT_BACKUP_PATH
  cp -r $CERT_ROOT_PATH/cluster* $CERT_BACKUP_PATH/
  cp -r $CERT_ROOT_PATH/ca* $CERT_BACKUP_PATH/
}

generate-new-certs() {
  echo "Generating new certs under a temp folder."
  mkdir $CERT_TEMP_PATH
  certm -d $CERT_TEMP_PATH ca generate -o=gateway --overwrite
  certm -d $CERT_TEMP_PATH server generate -o=gateway --host localhost --host 127.0.0.1
  mv $CERT_TEMP_PATH/server.pem $CERT_TEMP_PATH/cluster.pem
  mv $CERT_TEMP_PATH/server-key.pem $CERT_TEMP_PATH/cluster-key.pem
  rm $CERT_TEMP_PATH/ca-key.pem
}

restore-backup() {
  yes | cp -rf $CERT_BACKUP_PATH/* $CERT_ROOT_PATH
}

replace-with-new-cert() {
  yes | cp -rf $CERT_TEMP_PATH/* $CERT_ROOT_PATH
}

restart-nginx() {
  systemctl restart nginx
}

main() {
  if [[ "$1" == "prepare" ]]; then
    backup-current-cert-files
    generate-new-certs
  fi
  if [[ "$1" == "rotate" ]]; then
    replace-with-new-cert
    restart-nginx
    replace-jumpgate-key
  fi
  if [[ "$1" == "rollback" ]]; then
    restore-backup
    restart-nginx
    clear-temp-folders
    replace-jumpgate-key
  fi
  if [[ "$1" == "finalize" ]]; then
    clear-temp-folders
  fi
}

main "$@"