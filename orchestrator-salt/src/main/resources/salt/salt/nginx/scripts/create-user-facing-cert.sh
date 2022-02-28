#!/bin/bash

CERT_ROOT_PATH=/etc/certs-user-facing

certm -d ${CERT_ROOT_PATH} ca generate -o=gateway --overwrite
certm -d ${CERT_ROOT_PATH} server generate -o=gateway --host hostname --host {{ salt['pillar.get']('gateway:address') }}
chown root:root ${CERT_ROOT_PATH}/*
chmod 600 ${CERT_ROOT_PATH}/*