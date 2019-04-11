#!/bin/bash

enable_tls_ambari() {
  ambari-server setup-security --security-option=setup-https --api-ssl=true --api-ssl-port=8443 --import-cert-path=/etc/certs/cluster.pem --import-key-path=/etc/certs/cluster-key.pem --pem-password=
  sed -i 's/8080/8443/' /etc/nginx/nginx.conf
  sed -i 's/http:\/\/ambari/https:\/\/ambari/' /etc/nginx/sites-enabled/ssl.conf
  pkill -HUP nginx
}

main() {
  enable_tls_ambari
}

main