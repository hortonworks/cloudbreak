#!/usr/bin/env bash

set +x

: ${INTEGCB_LOCATION?"integcb location"}

cd $INTEGCB_LOCATION

sudo ./cbd regenerate
sudo ./cbd start commondb

echo "Revert CB database test"

sudo rm -rf .schema
sudo ./cbd migrate cbdb pending
sudo rm -rf .schema
sudo ./cbd migrate cbdb down 10 > cbdb-revert.result 2>&1

cat cbdb-revert.result

if grep -q ERROR "cbdb-revert.result" || grep -q 'Permission denied' "cbdb-revert.result";
    then echo -e "\033[0;91m--- !!! REVERT CB DB FAILED !!! ---\n"; exit 1;
    else echo -e "\n\033[0;92m+++ !!! REVERT CB DB SUCCESSFULLY FINISHED !!! +++\n";
fi

echo "Revert ENVIRONMENT database test"

sudo rm -rf .schema
sudo ./cbd migrate environmentdb pending
sudo rm -rf .schema
sudo ./cbd migrate environmentdb down 10 > environmentdb-revert.result 2>&1

cat environmentdb-revert.result

if grep -q ERROR "environmentdb-revert.result" || grep -q 'Permission denied' "environmentdb-revert.result";
    then echo -e "\033[0;91m--- !!! REVERT ENVIRONMENT DB FAILED !!! ---\n"; exit 1;
    else echo -e "\n\033[0;92m+++ !!! REVERT ENVIRONMENT DB SUCCESSFULLY FINISHED !!! +++\n";
fi

echo "Revert FREEIPA database test"

sudo rm -rf .schema
sudo ./cbd migrate freeipadb pending
sudo rm -rf .schema
sudo ./cbd migrate freeipadb down 10 > freeipadb-revert.result 2>&1

cat freeipadb-revert.result

if grep -q ERROR "freeipadb-revert.result" || grep -q 'Permission denied' "freeipadb-revert.result";
    then echo -e "\033[0;91m--- !!! REVERT FREEIPA DB FAILED !!! ---\n"; exit 1;
    else echo -e "\n\033[0;92m+++ !!! REVERT FREEIPA DB SUCCESSFULLY FINISHED !!! +++\n";
fi

echo "Revert REDBEAMS database test"

sudo rm -rf .schema
sudo ./cbd migrate redbeamsdb pending
sudo rm -rf .schema
sudo ./cbd migrate redbeamsdb down 10 > redbeamsdb-revert.result 2>&1

cat redbeamsdb-revert.result

if grep -q ERROR "redbeamsdb-revert.result" || grep -q 'Permission denied' "redbeamsdb-revert.result";
    then echo -e "\033[0;91m--- !!! REVERT REDBEAMS DB FAILED !!! ---\n"; exit 1;
    else echo -e "\n\033[0;92m+++ !!! REVERT REDBEAMS DB SUCCESSFULLY FINISHED !!! +++\n";
fi

echo "Revert DATALAKE database test"

sudo rm -rf .schema
sudo ./cbd migrate datalakedb pending
sudo rm -rf .schema
sudo ./cbd migrate datalakedb down 10 > datalakedb-revert.result 2>&1

cat datalakedb-revert.result

if grep -q ERROR "datalakedb-revert.result" || grep -q 'Permission denied' "datalakedb-revert.result";
    then echo -e "\033[0;91m--- !!! REVERT DATALAKE DB FAILED !!! ---\n"; exit 1;
    else echo -e "\n\033[0;92m+++ !!! REVERT DATALAKE DB SUCCESSFULLY FINISHED !!! +++\n";
fi

echo "Revert EXTERNALIZED_COMPUTE database test"

sudo rm -rf .schema
sudo ./cbd migrate externalizedcomputedb pending
sudo rm -rf .schema
sudo ./cbd migrate externalizedcomputedb down 10 > externalizedcomputedb-revert.result 2>&1

cat externalizedcomputedb-revert.result

if grep -q ERROR "externalizedcomputedb-revert.result" || grep -q 'Permission denied' "externalizedcomputedb-revert.result";
    then echo -e "\033[0;91m--- !!! REVERT EXTERNALIZED_COMPUTE DB FAILED !!! ---\n"; exit 1;
    else echo -e "\n\033[0;92m+++ !!! REVERT EXTERNALIZED_COMPUTE DB SUCCESSFULLY FINISHED !!! +++\n";
fi