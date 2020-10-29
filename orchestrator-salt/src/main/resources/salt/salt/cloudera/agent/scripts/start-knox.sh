#!/bin/bash

set -x
set -e

KNOX_PROC_FILE=/opt/salt/knox-process-name

if [ -f "$KNOX_PROC_FILE" ]; then
  systemctl status cloudera-scm-agent.service || :
  KNOX=$(head -n 1 $KNOX_PROC_FILE)
  echo 0 > /var/run/cloudera-scm-agent/process/$KNOX/exit_code
  if [ ! -f "/var/run/cloudera-scm-agent/supervisor/include/$KNOX.conf" ]; then
    ln -s /var/run/cloudera-scm-agent/process/$KNOX/supervisor.conf /var/run/cloudera-scm-agent/supervisor/include/$KNOX.conf
  fi
  /opt/cloudera/cm-agent/bin/supervisorctl -c /var/run/cloudera-scm-agent/supervisor/supervisord.conf reread
  /opt/cloudera/cm-agent/bin/supervisorctl -c /var/run/cloudera-scm-agent/supervisor/supervisord.conf add $KNOX
  rm -f $KNOX_PROC_FILE
fi