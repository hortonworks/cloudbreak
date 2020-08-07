#!/usr/bin/env bash

date

echo "Replacing UMS_HOST in profile file"

CRED_FILE="./src/main/resources/ums-users/api-credentials.json"
if [ ! -f "$CRED_FILE" ]; then
    echo "$CRED_FILE does not exist. Make sure to run 'make fetch-secrets'"
    exit 1
fi

sed '/export UMS_HOST/d' ./integcb/Profile > ./integcb/Profile.tmp1
sed '/export CB_JAVA_OPTS/d' ./integcb/Profile.tmp1 > ./integcb/Profile.tmp
mv ./integcb/Profile.tmp ./integcb/Profile

#export CB_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5 -Dthunderhead.url=thunderhead-mock:8080 -Drest.debug=true -Dmock.spi.endpoint=https://test:9443"

echo 'export UMS_HOST="ums.thunderhead-dev.cloudera.com"' >> integcb/Profile
echo 'export CB_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5 -Drest.debug=true -Dmock.spi.endpoint=https://test:9443"' >> integcb/Profile
echo 'export REDBEAMS_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
echo 'export DATALAKE_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
echo 'export FREEIPA_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
echo 'export ENVIRONMENT_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile

echo "Replacement done with UMS_HOST and JAVA OPTS for altus.ums.rights.cache.seconds"




