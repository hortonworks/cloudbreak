#!/bin/bash

set -x

SERVICE_FILE=/lib/systemd/system/krb5-admin-server.service

if [[ -f "$SERVICE_FILE" ]]; then
  if grep "/etc/krb5kdc" "$SERVICE_FILE"; then
    echo "$SERVICE_FILE contains the /etc/krb5kdc directory"
  else
    sed -i 's/ReadWriteDirectories=/ReadWriteDirectories=\/etc\/krb5kdc /g' $SERVICE_FILE
  fi
fi