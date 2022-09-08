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

if [ -f "/etc/httpd/alias/cert8.db" ]; then
  log "Generate privateKey.pem and publicCert.pem by Server-Cert in /etc/httpd/alias/cert8.db"
  CERT_FILE=$BASE_PATH/cert.p12

  pk12util -o $CERT_FILE -n 'Server-Cert' -d /etc/httpd/alias -k /etc/httpd/alias/pwdfile.txt -W ""
  openssl pkcs12 -in $CERT_FILE -nocerts -out $PRIVATE_CERT_PEM_FILE -nodes -passout pass: -passin pass:
  openssl pkcs12 -in $CERT_FILE -clcerts -nokeys -out $PUBLIC_CERT_PEM_FILE -passin pass:
  rm -f $CERT_FILE
else
  log "/etc/httpd/alias/cert8.db is missing"

  PASSWORD_FILE=/var/lib/ipa/passwds/$(hostname)-443-RSA
  KEY_FILE=/var/lib/ipa/private/httpd.key

  if [ ! -f "$KEY_FILE" ]; then
    log "$KEY_FILE is missing, unable to generate privateKey.pem"
  elif [ ! -f "$PASSWORD_FILE" ]; then
    log "$PASSWORD_FILE is missing, unable to generate privateKey.pem"
  else
    log "Generate privateKey.pem by $KEY_FILE and $PASSWORD_FILE"
    openssl rsa -in $KEY_FILE -passin file:$PASSWORD_FILE -text > $$PRIVATE_CERT_PEM_FILE
  fi

  CRT_FILE=/var/lib/ipa/certs/httpd.crt
  if [ ! -f "$CRT_FILE" ]; then
    log "$CRT_FILE is missing, unable to generate publicCert.pem"
  else
    log "Generate publicCert.pem by $CRT_FILE"
    openssl x509 -in $CRT_FILE -out $PUBLIC_CERT_PEM_FILE -outform PEM
  fi
fi

if [ ! -f "$PUBLIC_CERT_PEM_FILE" ]; then
  log "$PUBLIC_CERT_PEM_FILE is missing"
  exit 1
fi
if [ ! -f "$PRIVATE_CERT_PEM_FILE" ]; then
  log "$PRIVATE_CERT_PEM_FILE is missing"
  exit 1
fi

chmod 600 $PRIVATE_CERT_PEM_FILE $PUBLIC_CERT_PEM_FILE

systemctl restart cdp-freeipa-healthagent