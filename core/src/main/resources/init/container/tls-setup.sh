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
  CBD_CERT_ROOT_PATH=/etc/certs
  DOCKER_TAG_CERT_TOOL=0.2.0
  sudo docker run --rm -v $CBD_CERT_ROOT_PATH:/certs ehazlett/certm:$DOCKER_TAG_CERT_TOOL -d /certs ca generate -o=gateway
  sudo docker run --rm -v $CBD_CERT_ROOT_PATH:/certs ehazlett/certm:$DOCKER_TAG_CERT_TOOL -d /certs client generate --common-name=${publicIp} -o=gateway

  while [ ! -f $CBD_CERT_ROOT_PATH/key.pem ] || [ ! -f $CBD_CERT_ROOT_PATH/cert.pem ]; do
    sleep 1
  done

  sudo mv $CBD_CERT_ROOT_PATH/cert.pem $CBD_CERT_ROOT_PATH/cluster.pem
  sudo cp /etc/certs/cluster.pem /tmp/cluster.pem
  sudo mv $CBD_CERT_ROOT_PATH/key.pem $CBD_CERT_ROOT_PATH/cluster-key.pem
  sudo rm $CBD_CERT_ROOT_PATH/ca-key.pem
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
