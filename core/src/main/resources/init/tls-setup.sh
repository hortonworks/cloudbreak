#!/bin/bash
((
set -x
sudo mkdir -p /etc/certs
sudo cp /tmp/cb-client.pem /etc/certs
MAX_RETRIES=60
retries=0
while ((retries++ < MAX_RETRIES)) && ! sudo docker info &> /dev/null; do echo "Docker is not running yet."; sleep 5; done
sudo docker run --rm -v /etc/certs:/certs ehazlett/cert-tool -d /certs -o=gateway -s localhost -s 127.0.0.1 -s $PUBLIC_IP
sleep 5
sudo mv /etc/certs/server-key.pem /etc/certs/server.key
sudo docker run --name gateway -d --net=host --restart=always -v /etc/certs:/certs sequenceiq/cb-gateway-nginx:0.2
) 2>&1) | sudo tee /var/log/tls-setup.log