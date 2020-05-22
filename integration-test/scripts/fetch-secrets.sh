#!/usr/bin/env bash

if [[ -z $AZURE_CLIENT_ID ]]; then
  echo "Variable AZURE_CLIENT_ID not set, using az login."
  az login
else
  az login -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --service-principal --tenant $AZURE_TENANT_ID
fi

mkdir -p ./src/main/resources/ums-users

az keyvault secret show --name "real-ums-users-dev" --vault-name "jenkins-secret" --query 'value' -o tsv | jq >> ./src/main/resources/ums-users/api-credentials.json

