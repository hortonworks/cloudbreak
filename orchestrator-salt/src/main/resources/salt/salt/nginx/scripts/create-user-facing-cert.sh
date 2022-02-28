#!/bin/bash

set -e

CERT_ROOT_PATH=/etc/certs-user-facing

openssl genrsa -out ${CERT_ROOT_PATH}/ca-key.pem 2048
openssl req -new -x509 -key ${CERT_ROOT_PATH}/ca-key.pem -out ${CERT_ROOT_PATH}/ca.pem -config ${CERT_ROOT_PATH}/openssl.cnf -days 825

openssl genrsa -out ${CERT_ROOT_PATH}/server-key.pem 2048
openssl req -new -key ${CERT_ROOT_PATH}/server-key.pem -out ${CERT_ROOT_PATH}/server.csr -subj '/O=cloudera'
openssl x509 -req -in ${CERT_ROOT_PATH}/server.csr -CA ${CERT_ROOT_PATH}/ca.pem -CAkey ${CERT_ROOT_PATH}/ca-key.pem -out ${CERT_ROOT_PATH}/server.pem -CAcreateserial -CAserial ${CERT_ROOT_PATH}/ca.srl -days 825 -sha256 -extfile ${CERT_ROOT_PATH}/server_cert_ext.cnf

rm -rf ${CERT_ROOT_PATH}/ca.srl
rm -rf ${CERT_ROOT_PATH}/server.csr

chown root:root ${CERT_ROOT_PATH}/*
chmod 600 ${CERT_ROOT_PATH}/*