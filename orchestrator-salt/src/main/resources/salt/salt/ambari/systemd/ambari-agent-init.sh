#!/bin/bash

set -x

: ${CLOUD_PLATFORM:="none"}
: ${USE_CONSUL_DNS:="true"}
: ${AMBARI_SERVER_ADDR:="ambari-server.service.consul"}

debug() {
  [[ "DEBUG" ]]  && echo "[DEBUG] $@" 1>&2
}

get_nameserver_addr() {
  if [[ "$NAMESERVER_ADDR" ]]; then
    echo $NAMESERVER_ADDR
  else
    if ip addr show docker0 &> /dev/null; then
      ip addr show docker0 | grep "inet\b" | awk '{print $2}' | cut -d/ -f1
    else
      ip ro | grep default | cut -d" " -f 3
    fi
  fi
}

ambari_server_addr() {
  sed -i "s/^hostname=.*/hostname=${AMBARI_SERVER_ADDR}/" /etc/ambari-agent/conf/ambari-agent.ini
}

main() {
  ambari_server_addr
}

main "$@"
