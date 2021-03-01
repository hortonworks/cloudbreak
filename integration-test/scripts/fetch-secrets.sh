#!/usr/bin/env bash

set -ex

secret_version="d7f9f1439406482385174236dcf02477"

USER_JSON_LOCATION="./src/main/resources/ums-users/api-credentials.json"

if [[ -z $AZURE_CLIENT_ID ]]; then
  echo "Variable AZURE_CLIENT_ID not set, using az login."
  az login
else
  az login -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --service-principal --tenant $AZURE_TENANT_ID
fi

mkdir -p ./src/main/resources/ums-users

echo "Executing secret fetching from Azure 'jenkins-secret' store"
az keyvault secret show --name "real-ums-users-dev" --vault-name "jenkins-secret" --version $secret_version --query 'value' -o tsv | jq > $USER_JSON_LOCATION

echo "Checking if valid json file was fetched: $USER_JSON_LOCATION"
cat $USER_JSON_LOCATION | jq type


