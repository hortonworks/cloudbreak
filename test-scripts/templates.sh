#!/bin/bash
echo "Testing template REST API"

TEMPLATE_ID=$(curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d @json/template.json http://localhost:8080/templates | jq '.id')

echo "Generated template with id: $TEMPLATE_ID"

curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/templates/$TEMPLATE_ID | jq .

echo "Delete template with id: $TEMPLATE_ID ..."
curl -u cbuser@sequenceiq.com:test123 -sX DELETE -H "Content-Type:application/json" http://localhost:8080/templates/$TEMPLATE_ID | jq .

curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/templates | jq .

