#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- download latest cbd\033[0m\n"
cd $INTEGCB_LOCATION
curl -L s3.amazonaws.com/public-repo-1.hortonworks.com/HDP/cloudbreak/cloudbreak-deployer_snapshot_$(uname)_x86_64.tgz | tar -xz
