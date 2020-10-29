#!/bin/bash

set -x

echo "Process name: $PROC_NAME"

KNOX=$(/opt/cloudera/cm-agent/bin/supervisorctl -c /var/run/cloudera-scm-agent/supervisor/supervisord.conf  status | awk -v procName="$PROC_NAME" '$0~procName {print $1}')

if [ -n "$KNOX" ]; then
  /opt/cloudera/cm-agent/bin/supervisorctl -c /var/run/cloudera-scm-agent/supervisor/supervisord.conf stop $KNOX
  echo "$KNOX" > /opt/salt/knox-process-name
fi
