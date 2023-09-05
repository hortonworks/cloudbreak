#!/usr/bin/env bash
set -x

CERTMANAGER_DIR="/etc/cloudera-scm-server/certs"
rm -rf ${CERTMANAGER_DIR}_bkp

target_file=/etc/cloudera-scm-server/cm.settings
rm ${target_file}_bkp

