#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- Create cloudbreak context\033[0m\n"
cp $INTEGCB_LOCATION/Profile_template $INTEGCB_LOCATION/Profile
cp docker-compose_template.yml docker-compose.yml
./scripts/fill_public_ip.sh
cd $INTEGCB_LOCATION
