#!/bin/bash

setup_cbclient_cert() {
  sudo mkdir -p /etc/certs
  sudo cp /tmp/cb-client.pem /etc/certs
}

create_certificates() {
  sudo cert-tool -d=/etc/certs -o=gateway -s localhost -s 127.0.0.1 -s ${publicIp}
  sudo rm /etc/certs/client-key.pem /etc/certs/client.pem /etc/certs/ca-key.pem
  sudo cp /etc/certs/server.pem /tmp/server.pem
}

start_nginx() {
  sudo mv /tmp/nginx.conf /etc/nginx/nginx.conf
  sudo mkdir -p /usr/share/nginx/json/
  sudo mv /tmp/50x.json /usr/share/nginx/json/50x.json
  sudo service nginx start
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
