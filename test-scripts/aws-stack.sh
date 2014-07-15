#!/bin/bash
echo "Testing stack REST API"

STACK_ID=$(curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d @json/aws-stack.json http://localhost:8080/stacks | jq '.id')

echo "Delete stack with id $STACK_ID ..."
curl -u cbuser@sequenceiq.com:test123 -X DELETE -H "Content-Type:application/json" http://localhost:8080/stacks/$STACK_ID | jq .

curl -u cbuser@sequenceiq.com:test123 -X GET -H "Content-Type:application/json" http://localhost:8080/stacks/| jq .