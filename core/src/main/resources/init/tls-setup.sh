#!/bin/bash

setup_cbclient_cert() {
  ${sudopre} sudo ${sudocheck} mkdir -p /etc/certs
  ${sudopre} sudo ${sudocheck} cp /tmp/cb-client.pem /etc/certs
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
  ${sudopre} sudo ${sudocheck} docker run --rm -v /etc/certs:/certs ehazlett/cert-tool:0.0.3 -d /certs -o=gateway -s localhost -s 127.0.0.1 -s ${publicIp}
  while [ ! -f /etc/certs/server-key.pem ] || [ ! -f /etc/certs/server.pem ]; do
    sleep 1
  done
  ${sudopre} sudo ${sudocheck} mv /etc/certs/server-key.pem /etc/certs/server.key
  ${sudopre} sudo ${sudocheck} sh -c 'cat /etc/certs/ca.pem >> /etc/certs/server.pem'
  ${sudopre} sudo ${sudocheck} rm /etc/certs/client-key.pem /etc/certs/client.pem /etc/certs/ca-key.pem
  ${sudopre} sudo ${sudocheck} cp /etc/certs/server.pem /tmp/server.pem
}

start_nginx_container() {
  ${sudopre} sudo ${sudocheck} docker run --name gateway -d --net=host --restart=always -v /etc/certs:/certs sequenceiq/cb-gateway-nginx:0.3
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
