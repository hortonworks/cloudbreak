#!/bin/bash

setup_cbclient_cert() {
  sudo mkdir -p /etc/certs
  sudo cp /tmp/cb-client.pem /etc/certs
}

create_certificates_cert_tool() {
  echo n | sudo cert-tool -d=/etc/certs -o=gateway -s localhost -s 127.0.0.1 -s ${publicIp}
  sudo rm /etc/certs/client-key.pem /etc/certs/client.pem /etc/certs/ca-key.pem
  sudo mv /etc/certs/server.pem /etc/certs/cluster.pem
  sudo cp /etc/certs/cluster.pem /tmp/cluster.pem
  sudo mv /etc/certs/server-key.pem /etc/certs/cluster-key.pem
}

create_certificates_certm() {

  CBD_CERT_ROOT_PATH=/etc/certs

  sudo certm -d $CBD_CERT_ROOT_PATH ca generate -o=gateway --overwrite
  sudo certm -d $CBD_CERT_ROOT_PATH server generate -o=gateway --host localhost --host 127.0.0.1 --host ${publicIp}
  sudo mv $CBD_CERT_ROOT_PATH/server.pem $CBD_CERT_ROOT_PATH/cluster.pem
  sudo cp $CBD_CERT_ROOT_PATH/cluster.pem /tmp/cluster.pem
  sudo mv $CBD_CERT_ROOT_PATH/server-key.pem $CBD_CERT_ROOT_PATH/cluster-key.pem
  sudo rm $CBD_CERT_ROOT_PATH/ca-key.pem
}

start_nginx() {
  sudo mkdir -p /etc/nginx/sites-enabled/
  sudo mv /tmp/ssl.conf /etc/nginx/sites-enabled/ssl.conf
  sudo mkdir -p /usr/share/nginx/json/
  # FIXME (BUG-92636): CLOUD_PLATFORM is not provided, need to check for YCloud through cloudbreak-config.props
  # FIXME (BUG-92276): It is not possible to restart nginx if systemd is non pid-1
  if [[ -d /yarn-private ]]; then
      sudo pkill -1 -P 1 nginx
  else
      sudo service nginx restart
  fi
  sudo chkconfig nginx on
}

setup_tls() {
  setup_cbclient_cert
  if [[ -f /sbin/certm ]]
  then
    echo "certm exists on the fs"
    create_certificates_certm
  else
    echo "cert-tool exists on the fs (backward compatibility)"
    create_certificates_cert_tool
  fi
  start_nginx
}

main() {
  set -x
  setup_tls 2>&1 | tee /home/${username}/tls-setup.log
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"