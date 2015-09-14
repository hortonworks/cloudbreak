#!/bin/bash -e

: ${LOGFILE:=/var/log/consul-watch/consul_handler.log}

main(){
  echo "Getting p12 file from Consul's key-value store"
  curl "http://$CONSUL_HOST:$CONSUL_HTTP_PORT/v1/kv/privateKeyEncoded?raw" | base64 -d > /usr/lib/hadoop/lib/gcp.p12
  echo "p12 file successfully downloaded from key-value store."
}

exec &> "$LOGFILE"
[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
