#!/usr/bin/env bash

set -ex

: ${SECRET_VERSION:="e556a55296e349f993d324680baa3350"}
: ${USER_JSON_LOCATION:="./src/main/resources/ums-users/api-credentials.json"}
: ${USER_JSON_SECRET:="real-ums-users-dev"}

init-secret-parameters() {
    echo "Setting up UMS users secret parameters based on environment variables..."
    if [[ ! -z $INTEGRATIONTEST_UMS_JSONSECRET_VERSION ]]; then
        SECRET_VERSION=$INTEGRATIONTEST_UMS_JSONSECRET_VERSION
    fi
    if [[ ! -z $INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH ]]; then
        USER_JSON_LOCATION=$INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH
    fi
    if [[ ! -z $INTEGRATIONTEST_UMS_JSONSECRET_NAME ]]; then
        USER_JSON_SECRET=$INTEGRATIONTEST_UMS_JSONSECRET_NAME
    fi
}

init-ums-users-temp() {
    USER_JSON_FOLDER=${USER_JSON_LOCATION%/*}
    echo "Updating UMS users Json file..."
    if [[ ! -d $USER_JSON_FOLDER ]]; then
        mkdir -p $USER_JSON_FOLDER
    elif [[ -f $USER_JSON_LOCATION ]]; then
        rm -f $USER_JSON_LOCATION
    fi
}

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

fetch-real-ums-users() {
    if [[ "$GITHUB_ENV" == false ]]; then
      echo "Fetching Manowar Dev Real UMS Users from Azure 'jenkins-secret' key vault..."
      az keyvault secret show --name $USER_JSON_SECRET --vault-name "jenkins-secret" --version $SECRET_VERSION --query 'value' -o tsv | jq '.' > $USER_JSON_LOCATION
    else
      echo $REAL_UMS_USERS_DEV| jq '.' > $USER_JSON_LOCATION
    fi
    echo "Validate Real UMS User Store: File should be a valid JSON at: $USER_JSON_LOCATION"
    cat $USER_JSON_LOCATION | jq type
}

main() {
  init-secret-parameters
  init-ums-users-temp
  init-azure-auth
  fetch-real-ums-users
}

main "$@"