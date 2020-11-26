#!/usr/bin/env bash

: "${SOCKET_WAIT_LIMIT:=1000}"
socket_wait_count=$(netstat -nat -p | grep ':9443\|:443' | grep CLOSE_WAIT | wc -l)
if [[ $socket_wait_count -ge $SOCKET_WAIT_LIMIT ]]; then
  echo "$(date '+%d/%m/%Y %H:%M:%S') - socket wait count ($socket_wait_count) is higher than the limit ($SOCKET_WAIT_LIMIT)" |& tee -a /var/log/socket_wait_cleanup.log
  systemctl restart ccm-tunnel@*
  echo "$(date '+%d/%m/%Y %H:%M:%S') - CCM tunnel services was restarted" |& tee -a /var/log/socket_wait_cleanup.log
else
  echo "$(date '+%d/%m/%Y %H:%M:%S') - socket wait count ($socket_wait_count) is lower than the limit ($SOCKET_WAIT_LIMIT) " |& tee -a /var/log/socket_wait_cleanup.log
fi
