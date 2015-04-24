#!/bin/bash
echo "Testing azure template REST API"

AZURE_TEMPLATE_ID=$(curl --config curl_user_config -d @json/azure-template.json http://localhost:8080/templates | jq '.id')
echo "Generated azure template with id: $AZURE_TEMPLATE_ID"

curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/templates/$AZURE_TEMPLATE_ID | jq .

echo "Delete azure template with id: $AZURE_TEMPLATE_ID ..."
curl -u cbuser@sequenceiq.com:test123 -sX DELETE -H "Content-Type:application/json" http://localhost:8080/templates/$AZURE_TEMPLATE_ID | jq .

curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/templates | jq .