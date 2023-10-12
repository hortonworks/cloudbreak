#!/usr/bin/env bash

CERTMANAGER_DIR="/etc/cloudera-scm-server/certs"
CM_SETTINGS_FILE=/etc/cloudera-scm-server/cm.settings

echo "$(date '+%d/%m/%Y %H:%M:%S') - Cleaning up leftover after CMCA renewal."
rm -rf ${CERTMANAGER_DIR}_bkp_*
rm -f ${CM_SETTINGS_FILE}.bkp.*

