#!/bin/bash

setup_cbclient_cert() {
  sudo mkdir -p /etc/certs
  sudo cp /tmp/cb-client.pem /etc/certs
}

waiting_for_docker() {
  MAX_RETRIES=60
  retries=0
  while ((retries++ < MAX_RETRIES)) && ! sudo docker info &> /dev/null; do
    echo "Docker is not running yet."
    sleep 5
  done
}

create_certificates() {
  sudo docker run --rm -v /etc/certs:/certs ehazlett/cert-tool:0.0.3 -d /certs -o=gateway -s localhost -s 127.0.0.1 -s $PUBLIC_IP
  while [ ! -f /etc/certs/server-key.pem ] || [ ! -f /etc/certs/server.pem ]; do
    sleep 1
  done
  sudo mv /etc/certs/server-key.pem /etc/certs/server.key
  sudo sh -c 'cat /etc/certs/ca.pem >> /etc/certs/server.pem'
  sudo rm /etc/certs/client-key.pem /etc/certs/client.pem /etc/certs/ca-key.pem
  sudo cp /etc/certs/server.pem /tmp/server.pem
}

start_nginx_container() {
  sudo docker run --name gateway -d --net=host --restart=always -v /etc/certs:/certs sequenceiq/cb-gateway-nginx:0.2
}

setup_tls() {
  setup_cbclient_cert
  waiting_for_docker
  create_certificates
  start_nginx_container
}

main() {
  set -x
  setup_tls 2>&1 | sudo tee /var/log/tls-setup.log
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
