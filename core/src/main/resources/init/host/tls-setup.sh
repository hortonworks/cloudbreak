#!/bin/bash

setup_cbclient_cert() {
  sudo mkdir -p /etc/certs
  sudo cp /tmp/cb-client.pem /etc/certs
}

create_certificates() {

  CBD_CERT_ROOT_PATH=/etc/certs

  sudo cert-tool -d $CBD_CERT_ROOT_PATH ca generate -o=gateway --overwrite
  sudo cert-tool -d $CBD_CERT_ROOT_PATH client generate --common-name=${publicIp} -o=gateway --overwrite
  sudo mv $CBD_CERT_ROOT_PATH/cert.pem $CBD_CERT_ROOT_PATH/cluster.pem
  sudo cp /etc/certs/cluster.pem /tmp/cluster.pem
  sudo mv $CBD_CERT_ROOT_PATH/key.pem $CBD_CERT_ROOT_PATH/cluster-key.pem
  sudo rm $CBD_CERT_ROOT_PATH/ca-key.pem
}

start_nginx() {
  sudo mkdir -p /etc/nginx/sites-enabled/
  sudo mv /tmp/ssl.conf /etc/nginx/sites-enabled/ssl.conf
  sudo mkdir -p /usr/share/nginx/json/
  sudo service nginx restart
  sudo chkconfig nginx on
}

setup_tls() {
  setup_cbclient_cert
  create_certificates
  start_nginx
}

main() {
  set -x
  setup_tls 2>&1 | tee /home/${username}/tls-setup.log
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
