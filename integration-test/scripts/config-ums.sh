#!/usr/bin/env bash

set -ex

: ${CRED_FILE:="./src/main/resources/ums-users/api-credentials.json"}

init-secret-parameters() {
    echo "Setting up UMS users store based on environment variables..."
    if [[ ! -z $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH ]]; then
        CRED_FILE=$INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH
    fi
}

validate-ums-users() {
    if [ ! -f "$CRED_FILE" ]; then
        echo "$CRED_FILE does not exist. Make sure to run 'make fetch-secrets'"
        exit 1
    fi
}

prepare-cb-profile() {
    echo "Replacing UMS_HOST in profile file"
    sed '/export UMS_HOST/d' ./integcb/Profile > ./integcb/Profile.tmp1
    sed '/export CB_JAVA_OPTS/d' ./integcb/Profile.tmp1 > ./integcb/Profile.tmp
    mv ./integcb/Profile.tmp ./integcb/Profile
}

update-cb-profile() {
    echo 'export UMS_HOST="ums.thunderhead-dev.cloudera.com"' >> integcb/Profile
    echo 'export CB_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5 -Drest.debug=true -Dmock.spi.endpoint=https://test:9443"' >> integcb/Profile
    echo 'export REDBEAMS_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
    echo 'export DATALAKE_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
    echo 'export FREEIPA_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
    echo 'export ENVIRONMENT_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile

    echo "Replacement done with UMS_HOST and JAVA OPTS for altus.ums.rights.cache.seconds"
}

main() {
  date
  init-secret-parameters
  validate-ums-users
  prepare-cb-profile
  update-cb-profile
}

main "$@"



