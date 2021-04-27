#!/usr/bin/env bash

set -ex

: ${SECRET_VERSION:="69f803e3ff3c42d79be1f87bb04341e9"}
: ${USER_JSON_LOCATION:="./src/main/resources/ums-users/api-credentials.json"}
: ${USER_JSON_SECRET:="real-ums-users-dev"}

init-ums-users-temp() {
    if [[ -d ./src/main/resources/ums-users ]]; then
        rm -rf ./src/main/resources/ums-users
    fi
    mkdir -p ./src/main/resources/ums-users
}

init-azure-auth() {
    if [[ -z $AZURE_CLIENT_ID ]]; then
        echo "Azure Username and Password has not been set! So Azure Interactive Login has been initiated!"
        az login
    else
        az login -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --service-principal --tenant $AZURE_TENANT_ID
    fi
}

fetch-real-ums-users() {
    echo "Fetching Manowar Dev Real UMS Users from Azure 'jenkins-secret' key vault..."
    az keyvault secret show --name $USER_JSON_SECRET --vault-name "jenkins-secret" --version $SECRET_VERSION --query 'value' -o tsv | jq '.' > $USER_JSON_LOCATION

    echo "Validate Real UMS User Store: File should be a valid JSON at: $USER_JSON_LOCATION"
    cat $USER_JSON_LOCATION | jq type
}

main() {
  init-ums-users-temp
  init-azure-auth
  fetch-real-ums-users
}

main "$@"