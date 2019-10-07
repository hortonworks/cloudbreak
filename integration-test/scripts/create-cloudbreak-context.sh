#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

date
echo -e "\n\033[1;96m--- Create cloudbreak context\033[0m\n"
cp $INTEGCB_LOCATION/Profile_template $INTEGCB_LOCATION/Profile
cp docker-compose_template.yml docker-compose.yml
./scripts/fill_public_ip.sh
echo -e "\n" >> integcb/Profile
echo "export VAULT_AUTO_UNSEAL=true" >> integcb/Profile
echo "export VAULT_DB_SCHEMA=inet_vault_$(date +%s)" >> integcb/Profile
echo "export CB_UI_MAX_WAIT=600" >> integcb/Profile
cd $INTEGCB_LOCATION
