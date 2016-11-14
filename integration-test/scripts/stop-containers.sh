#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- Stop cbd containers"
cd $INTEGCB_LOCATION;
.deps/bin/docker-compose stop;
echo -e "\n\033[1;96m--- Save cloudbreak log to cloudbreak.log file"
docker logs cbreak_cloudbreak_1 &> ../cloudbreak.log;
