#!/usr/bin/env bash

set +x

: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- Create cloudbreak context\033[0m\n"
cp $INTEGCB_LOCATION/Profile_template $INTEGCB_LOCATION/Profile
cp docker-compose_template.yml docker-compose.yml
ip_address=$(docker run --label cbreak.sidekick=true alpine sh -c 'ip ro | grep default | cut -d" " -f 3')
sed -i.bak "s/dns:/dns: $ip_address/g" docker-compose.yml
./scripts/fill_public_ip.sh
cd $INTEGCB_LOCATION
./cbd delete
./cbd regenerate
.deps/bin/docker-compose stop
.deps/bin/docker-compose kill