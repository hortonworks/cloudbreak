#!/bin/bash

setup_cbclient_cert() {
  ${sudopre} sudo ${sudocheck} mkdir -p /etc/certs
  ${sudopre} sudo ${sudocheck} cp /tmp/cb-client.pem /etc/certs
}

create_certificates() {
  ${sudopre} sudo ${sudocheck} cert-tool -d=/etc/certs -o=gateway -s localhost -s 127.0.0.1 -s ${publicIp}
  ${sudopre} sudo ${sudocheck} mv /etc/certs/server-key.pem /etc/certs/server.key
  ${sudopre} sudo ${sudocheck} sh -c 'cat /etc/certs/ca.pem >> /etc/certs/server.pem'
  ${sudopre} sudo ${sudocheck} rm /etc/certs/client-key.pem /etc/certs/client.pem /etc/certs/ca-key.pem
  ${sudopre} sudo ${sudocheck} cp /etc/certs/server.pem /tmp/server.pem
}

start_nginx() {
  ${sudopre} sudo ${sudocheck} mv /tmp/nginx.conf /etc/nginx/nginx.conf
  ${sudopre} sudo ${sudocheck} mkdir -p /usr/share/nginx/json/
  ${sudopre} sudo ${sudocheck} mv /tmp/50x.json /usr/share/nginx/json/50x.json
  ${sudopre} sudo ${sudocheck} service nginx start
  ${sudopre} sudo ${sudocheck} chkconfig nginx on
}

untar_salt() {
  MAX_RETRIES=60
  retries=0
  while ((retries++ < MAX_RETRIES)) && [ ! -d "/tmp/salt" ]; do
    echo Trying to unzip salt.zip
    cd /tmp && unzip salt.zip &> /dev/null
    sleep 2
  done
  ${sudopre} sudo ${sudocheck} cp -r /tmp/salt/ /srv
  ${sudopre} sudo ${sudocheck} cp -r /tmp/pillar/ /srv
}

setup_tls() {
  setup_cbclient_cert
  create_certificates
  start_nginx
  untar_salt
}

main() {
  set -x
  setup_tls 2>&1 | tee /home/${username}/tls-setup.log
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
