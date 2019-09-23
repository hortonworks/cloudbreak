#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

echo -e "\n\033[1;96m--- Stop cbd containers"
cd $INTEGCB_LOCATION;
.deps/bin/docker-compose stop;
echo -e "\n\033[1;96m--- Save Cloudbreak log to cloudbreak.log file"
docker logs cbreak_cloudbreak_1 &> ../cloudbreak.log;
echo -e "\n\033[1;96m--- Save FreeIPA log to freeipa.log file"
docker logs cbreak_freeipa_1 &> ../freeipa.log;
echo -e "\n\033[1;96m--- Save Environment log to environment.log file"
docker logs cbreak_environment_1 &> ../environment.log;
echo -e "\n\033[1;96m--- Save SDX log to datalake.log file"
docker logs cbreak_datalake_1 &> ../datalake.log;
echo -e "\n\033[1;96m--- Save Redbeams log to redbeams.log file"
docker logs cbreak_redbeams_1 &> ../redbeams.log;
