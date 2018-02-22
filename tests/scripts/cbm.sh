#!/usr/bin/env bash

echo
echo "Preparing mock environment"
echo "--------------------------"
docker-compose up -d cloudbreak identity traefik
echo " Waiting for the environment (about 30 sec)"
sleep 30s
