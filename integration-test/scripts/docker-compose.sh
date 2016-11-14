#!/usr/bin/env bash

set +x

: ${INTEGCB_LOCATION?"integcb location"}

cd $INTEGCB_LOCATION
echo -e "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
./cbd kill

echo -e "\n\033[1;96m--- Kill running test container\033[0m\n"
cd ..
$INTEGCB_LOCATION/.deps/bin/docker-compose down

echo -e "\n\033[1;96m--- Start cloudbreak\033[0m\n"
cd $INTEGCB_LOCATION
./cbd start-wait consul registrator uaadb identity cbdb cloudbreak

echo -e "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
cd ..
rm -rf test-output
$INTEGCB_LOCATION/.deps/bin/docker-compose up test > test.out
echo -e "\n\033[1;96m--- Test finished\033[0m\n"