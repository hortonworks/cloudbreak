#!/bin/bash
echo "Testing aws cluster REST API"

AMI_ID=${1:-"ami-2f39f458"}
KEY_NAME=${2:-"sequence-eu"}
SSH_LOCATION=${3:-"0.0.0.0/0"}
ROLE_ARN=${4:-"arn:aws:iam::755047402263:role/seq-self-cf"}

AWS_TEMPLATE_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d '{"cloudPlatform":"AWS","name":"awsinstancestemplate","parameters":{"region":"EU_WEST_1","amiId": "'"$AMI_ID"'","keyName": "'"$KEY_NAME"'","sshLocation": "'"$SSH_LOCATION"'","instanceType":"T2Small"}}' http://localhost:8080/templates | jq '.id')
echo "Aws template id:  $AWS_TEMPLATE_ID"
CREDENTIAL_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d '{"name": "sqiq-0002","cloudPlatform": "AWS","description": "My aws credential","parameters": {"roleArn": "'"$ROLE_ARN"'"}}' http://localhost:8080/credentials | jq '.id')
echo "Credential id: $CREDENTIAL_ID"

STACK_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d '{"nodeCount":3,"templateId": "'"$AWS_TEMPLATE_ID"'","name":"sundaywork2","credentialId": "'"$CREDENTIAL_ID"'"}' http://localhost:8080/stacks | jq '.id')
echo "Created stack with id $STACK_ID"

CLUSTER_ID=$(curl -u cbuser@sequenceiq.com:test123 --config curl_config -d @json/aws-cluster.json http://localhost:8080/stacks/$STACK_ID/cluster | jq '.id')
echo "Created cluster with id $CLUSTER_ID"