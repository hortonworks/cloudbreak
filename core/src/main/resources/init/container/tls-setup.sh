#!/bin/bash

setup_cbclient_cert() {
  sudo mkdir -p /etc/certs
  sudo cp /tmp/cb-client.pem /etc/certs
}

waiting_for_docker() {
  if [ -f /var/docker-relocate ]; then
    MAX_RETRIES=120
  else
    MAX_RETRIES=60
  fi
  retries=0
  while ((retries++ < MAX_RETRIES)) && ! sudo docker info &> /dev/null; do
    echo "Docker is not running yet."
    sleep 5
  done
}

create_certificates() {
  sudo docker run --rm -v /etc/certs:/certs ehazlett/cert-tool:0.0.3 -d /certs -o=gateway -s localhost -s 127.0.0.1 -s ${publicIp}
  while [ ! -f /etc/certs/server-key.pem ] || [ ! -f /etc/certs/server.pem ]; do
    sleep 1
  done
  sudo rm /etc/certs/client-key.pem /etc/certs/client.pem /etc/certs/ca-key.pem
  sudo cp /etc/certs/server.pem /tmp/server.pem
}

start_nginx_container() {
  sudo docker run --name gateway -e PORT=${sslPort} -d --net=host --restart=always -v /etc/certs:/certs sequenceiq/cb-gateway-nginx:0.6
}

setup_tls() {
  setup_cbclient_cert
  waiting_for_docker
  create_certificates
  start_nginx_container
}

main() {
  set -x
  setup_tls 2>&1 | tee /home/${username}/tls-setup.log
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
