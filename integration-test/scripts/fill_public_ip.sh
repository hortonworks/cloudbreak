#!/usr/bin/env bash

PUBLIC_IP=`echo $DOCKER_HOST | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}'`
echo $PUBLIC_IP
echo $INTEGCB_LOCATION
if [[ "$PUBLIC_IP" ]]; then
	echo "export PUBLIC_IP=$PUBLIC_IP" >> integcb/Profile
	echo "export CB_HOST_ADDRESS=http://$PUBLIC_IP:8080" >> integcb/Profile
else
	echo "export PUBLIC_IP=127.0.0.1" >> integcb/Profile
	echo "export CB_HOST_ADDRESS=http://127.0.0.1:8080" >> integcb/Profile
fi
