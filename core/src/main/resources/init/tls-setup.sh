#!/bin/bash

MAX_RETRIES=60
retries=0
while ((retries++ < MAX_RETRIES)) && ! sudo docker info &> /dev/null; do echo "Docker is not running yet."; sleep 5; done
sudo docker run --name gateway -d --net=host --restart=always sequenceiq/cb-gateway-nginx:0.1