#!/bin/bash
echo "Testing azure cluster REST API"

# use your custom properties here
TEMPLATE_SSH_PUBLIC_KEY=$(cat $1) # content of an ssh public key file
TEMPLATE_LOCATION=$(2:-"NORTH_EUROPE")
TEMPLATE_IMAGE_NAME=$(3:-"sequnceiq-ambari-docker-v1")
TEMPLATE_PASSWORD=$(4:-"Password!@#$")
CREDENTIAL_SUBSCRIPTION_ID=$(5:-"1234-5678-1234-5678")
CREDENTIAL_JKS_PASSWORD=$(6:-"pw123")

AZURE_TEMPLATE_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d '{"cloudPlatform": "AZURE","name": "my-azure-cluster","description": "my azure template""parameters": {"location": "'"$TEMPLATE_LOCATION"'","imageName": "'"$TEMPLATE_IMAGE_NAME"'","password": "'"$TEMPLATE_PASSWORD"'","vmType": "SMALL","sshPublicKey": "'"$TEMPLATE_SSH_PUBLIC_KEY"'"}}' http://localhost:8080/templates | jq '.id')
echo "Generated azure template with id: $AZURE_TEMPLATE_ID"
AZURE_CREDENTIAL_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d '{"name": "sqiq-0001","cloudPlatform": "AZURE","description": "My azure credential","parameters": {"subscriptionId": "'"$CREDENTIAL_SUBSCRIPTION_ID"'","jksPassword": "'"$CREDENTIAL_JKS_PASSWORD"'"}}' http://localhost:8080/credentials | jq '.id')
echo "Generated azure credential with id: $AZURE_TEMPLATE_ID"

STACK_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d '{"nodeCount":3,"templateId": "'"$AZURE_TEMPLATE_ID"'","name":"sundaywork2","credentialId": "'"$AZURE_CREDENTIAL_ID"'"}' http://localhost:8080/stacks | jq '.id')
echo "Created stack with id $STACK_ID"

CLUSTER_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d @json/aws-cluster.json http://localhost:8080/stacks/$STACK_ID/cluster | jq '.id')
echo "Created cluster with id $CLUSTER_ID"