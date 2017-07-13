#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- download latest cbd\033[0m\n"
cd $INTEGCB_LOCATION

: ${branch:=rc-2.0}

circle_url=https://circleci.com/api/v1/project/hortonworks/cloudbreak-deployer
latest_build=$(curl -s ${circle_url}/tree/${branch}\?filter=completed\&limit=1 | grep -m 1 build_num | sed 's/[^0-9]*//g')
curl -sL $(curl -s ${circle_url}/${latest_build}/artifacts | grep url | grep -i $(uname) | cut -d\" -f 4) | tar -xz

cbd_version=$(./cbd --version)
echo -e "$cbd_version"