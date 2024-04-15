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
    sed -i '/export UMS_HOST/d' ./integcb/Profile
    sed -i '/export UMS_PORT/d' ./integcb/Profile
}

update-cb-profile() {
    echo "export UMS_HOST=$INTEGRATIONTEST_UMS_HOST" >> integcb/Profile
    echo "export UMS_PORT=$INTEGRATIONTEST_UMS_PORT" >> integcb/Profile
    sed -i '/^export CB_JAVA_OPTS/ s/.$/ -Daltus.ums.rights.cache.seconds.ttl=5 -Drest.debug=true -Dmock.spi.endpoint=https\:\/\/test\:9443\/"/' integcb/Profile
    echo 'export REDBEAMS_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
    echo 'export DATALAKE_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
    echo 'export FREEIPA_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
    echo 'export ENVIRONMENT_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile
    echo 'export REMOTE_ENVIRONMENT_JAVA_OPTS="-Daltus.ums.rights.cache.seconds.ttl=5"' >> integcb/Profile


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



