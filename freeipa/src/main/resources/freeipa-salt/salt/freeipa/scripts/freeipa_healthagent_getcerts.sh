#!/usr/bin/env bash

set -x
set -e

BASE_PATH=/cdp/ipahealthagent
LOG_FILE=/var/log/freeipa-healthagent-getcert.log
PUBLIC_CERT_PEM_FILE=$BASE_PATH/publicCert.pem
PRIVATE_CERT_PEM_FILE=$BASE_PATH/privateKey.pem

log() {
    echo $(date) $* >> $LOG_FILE
}

log "Generate pems for freeipa health agent"

certm -d $BASE_PATH ca generate -o=gateway --overwrite
certm -d $BASE_PATH server generate -o=gateway --host localhost --host 127.0.0.1 --overwrite
mv -f $BASE_PATH/server.pem $PUBLIC_CERT_PEM_FILE
mv -f $BASE_PATH/server-key.pem $PRIVATE_CERT_PEM_FILE
rm -f $BASE_PATH/ca.pem
rm -f $BASE_PATH/ca-key.pem

chmod 600 $PRIVATE_CERT_PEM_FILE $PUBLIC_CERT_PEM_FILE

systemctl restart cdp-freeipa-healthagent