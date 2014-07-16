#!/bin/bash
echo "Testing template REST API"

AWS_TEMPLATE_ID=$(curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d @json/aws-template.json http://localhost:8080/templates | jq '.id')

echo "Generated aws template with id: $AWS_TEMPLATE_ID"

curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/templates/$AWS_TEMPLATE_ID | jq .

echo "Delete aws template with id: $AWS_TEMPLATE_ID ..."
curl -u cbuser@sequenceiq.com:test123 -sX DELETE -H "Content-Type:application/json" http://localhost:8080/templates/$AWS_TEMPLATE_ID | jq .

curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/templates | jq .
