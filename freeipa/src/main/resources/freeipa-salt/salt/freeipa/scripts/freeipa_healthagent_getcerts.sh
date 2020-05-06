#!/usr/bin/env bash

BASE_PATH=/cdp/ipahealthagent
CERT_FILE=$BASE_PATH/cert.p12

pk12util -o $CERT_FILE -n 'Server-Cert' -d /etc/httpd/alias -k /etc/httpd/alias/pwdfile.txt -W ""
openssl pkcs12 -in $CERT_FILE -nocerts -out $BASE_PATH/privateKey.pem -nodes -passout pass: -passin pass:
openssl pkcs12 -in $CERT_FILE -clcerts -nokeys -out $BASE_PATH/publicCert.pem -passin pass:
rm -f $CERT_FILE
chmod 600 $BASE_PATH/privateKey.pem $BASE_PATH/publicCert.pem

systemctl enable freeipa-healthagent
systemctl restart freeipa-healthagent