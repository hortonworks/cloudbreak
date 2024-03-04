#!/usr/bin/env bash

PUBLIC_IP=`echo $DOCKER_HOST | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}'`
echo $PUBLIC_IP
echo $INTEGCB_LOCATION
echo -e "\n" >> integcb/Profile
if [[ "$PUBLIC_IP" ]]; then
	echo "export PUBLIC_IP=$PUBLIC_IP" >> integcb/Profile
	echo "export CB_HOST_ADDRESS=http://$PUBLIC_IP:8080" >> integcb/Profile
    echo "export ENVIRONMENT_HOST_ADDRESS=http://$PUBLIC_IP:8088" >> integcb/Profile
	echo "export FREEIPA_HOST_ADDRESS=http://$PUBLIC_IP:8090" >> integcb/Profile
	echo "export REDBEAMS_HOST_ADDRESS=http://$PUBLIC_IP:8087" >> integcb/Profile
	echo "export DATALAKE_HOST_ADDRESS=http://$PUBLIC_IP:8086" >> integcb/Profile
	echo "export PERISCOPE_HOST_ADDRESS=http://$PUBLIC_IP:8085" >> integcb/Profile
	echo "export EXTERNALIZED_COMPUTE_HOST_ADDRESS=http://$PUBLIC_IP:8091" >> integcb/Profile
else
	echo "export PUBLIC_IP=127.0.0.1" >> integcb/Profile
	echo "export CB_HOST_ADDRESS=http://127.0.0.1:8080" >> integcb/Profile
    echo "export ENVIRONMENT_HOST_ADDRESS=http://127.0.0.1:8088" >> integcb/Profile
	echo "export FREEIPA_HOST_ADDRESS=http://127.0.0.1:8090" >> integcb/Profile
	echo "export REDBEAMS_HOST_ADDRESS=http://127.0.0.1:8087" >> integcb/Profile
	echo "export DATALAKE_HOST_ADDRESS=http://127.0.0.1:8086" >> integcb/Profile
	echo "export PERISCOPE_HOST_ADDRESS=http://127.0.0.1:8085" >> integcb/Profile
	echo "export EXTERNALIZED_COMPUTE_HOST_ADDRESS=http://127.0.0.1:8091" >> integcb/Profile
fi
