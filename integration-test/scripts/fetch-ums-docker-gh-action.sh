#!/bin/bash -ex

: ${INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH:="./src/main/resources/ums-users/api-credentials.json"}
: ${INTEGRATIONTEST_UMS_JSONSECRET_NAME:="real-ums-users-dev"}

fetch-ums-users() {
    echo "Fetching Manowar Dev Real UMS Users from RELENG key vault..."
    mkdir -p ./src/main/resources/ums-users
    echo $REAL_UMS_USERS_DEV | jq '.' > $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH

    echo "Validate Real UMS User Store: File should be a valid JSON at: $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH"
    cat $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH | jq type
}

main() {
  fetch-ums-users
}

main "$@"