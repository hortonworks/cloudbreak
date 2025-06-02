#!/bin/bash -ex

export AZURE_CLIENT_ID="${AZURE_CLIENT_ID}"
export AZURE_CLIENT_SECRET="${AZURE_CLIENT_SECRET}"
export AZURE_TENANT_ID="${AZURE_TENANT_ID}"
: ${GITHUB_ENV:=false}
: ${INTEGRATIONTEST_UMS_JSONSECRET_VERSION:="e556a55296e349f993d324680baa3350"}
: ${INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH:="./src/main/resources/ums-users/api-credentials.json"}
: ${INTEGRATIONTEST_UMS_JSONSECRET_NAME:="real-ums-users-dev"}

init-azure-auth() {
  if [[ "$GITHUB_ENV" == false ]]; then
    if [[ -z $AZURE_CLIENT_ID ]]; then
        echo "Azure Username and Password has not been set! So Azure Interactive Login has been initiated!"
        az login
    else
        az login -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --service-principal --tenant $AZURE_TENANT_ID
    fi
  fi
}

fetch-ums-users() {
    if [[ "$GITHUB_ENV" == false ]]; then
      echo "Fetching Manowar Dev Real UMS Users from Azure 'jenkins-secret' key vault..."
      az keyvault secret show --name $INTEGRATIONTEST_UMS_JSONSECRET_NAME --vault-name "jenkins-secret" --version $INTEGRATIONTEST_UMS_JSONSECRET_VERSION --query 'value' -o tsv | jq '.' > $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH
    else
      echo "$REAL_UMS_USERS_DEV" > $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH
      cat $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH
    fi
    echo "Validate Real UMS User Store: File should be a valid JSON at: $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH"
    cat $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH | jq type
}

main() {
  init-azure-auth
  fetch-ums-users
}

main "$@"