#!/usr/bin/env bash

date

echo "Replacing UMS_HOST in profile file"

CRED_FILE="./src/main/resources/ums-users/api-credentials.json"
if [ ! -f "$CRED_FILE" ]; then
    echo "$CRED_FILE does not exist. Make sure to run 'make fetch-secrets'"
    exit 1
fi

sed '/export UMS_HOST/d' ./integcb/Profile > ./integcb/Profile.tmp
mv ./integcb/Profile.tmp ./integcb/Profile

echo "export UMS_HOST=ums.thunderhead-dev.cloudera.com" >> integcb/Profile

echo "Replacement done with UMS_HOST"




