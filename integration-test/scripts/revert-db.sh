#!/usr/bin/env bash

set +x

: ${INTEGCB_LOCATION?"integcb location"}

cd $INTEGCB_LOCATION

./cbd regenerate
./cbd start cbdb
sudo rm -rf .schema
./cbd migrate cbdb pending
sudo rm -rf .schema
./cbd migrate cbdb down 10 > revert.result 2>&1

cat revert.result

if grep -q ERROR "revert.result";
    then echo -e "\033[0;91m--- !!! REVERT DB FAILED !!! ---\n"; exit 1;
    else echo -e "\n\033[0;92m+++ !!! REVERT DB SUCCESSFULLY FINISHED !!! +++\n";
fi