#!/bin/bash -ex

: ${AZURE_CLIENT_ID:? required}
: ${AZURE_CLIENT_SECRET:? required}
: ${AZURE_TENANT_ID:? required}
: ${INTEGRATIONTEST_UMS_JSONSECRET_VERSION:="69f803e3ff3c42d79be1f87bb04341e9"}
: ${INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH:="./src/main/resources/ums-users/api-credentials.json"}
: ${INTEGRATIONTEST_UMS_JSONSECRET_NAME:="real-ums-users-dev"}

init-azure-auth() {
    if [[ -z $AZURE_CLIENT_ID ]]; then
        echo "Azure Username and Password has not been set! So Azure Interactive Login has been initiated!"
        az login
    else
        az login -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --service-principal --tenant $AZURE_TENANT_ID
    fi
}

fetch-ums-users() {
    echo "Fetching Manowar Dev Real UMS Users from Azure 'jenkins-secret' key vault..."
    az keyvault secret show --name $INTEGRATIONTEST_UMS_JSONSECRET_NAME --vault-name "jenkins-secret" --version $INTEGRATIONTEST_UMS_JSONSECRET_VERSION --query 'value' -o tsv | jq '.' > $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH

    echo "Validate Real UMS User Store: File should be a valid JSON at: $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH"
    cat $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH | jq type
}

main() {
  init-azure-auth
  fetch-ums-users
}

main "$@"