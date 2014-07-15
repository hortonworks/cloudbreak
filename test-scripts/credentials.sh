#!/bin/bash
echo "Testing credential REST API"

CREDENTIAL_ID=$(curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d @json/credential.json http://localhost:8080/credentials | jq '.id')

curl -u cbuser@sequenceiq.com:test123 -X GET -H "Content-Type:application/json" http://localhost:8080/credentials/$CREDENTIAL_ID | jq .

echo "Delete credential with id $CREDENTIAL_ID ..."
curl -u cbuser@sequenceiq.com:test123 -X DELETE -H "Content-Type:application/json" http://localhost:8080/credentials/$CREDENTIAL_ID | jq .

curl -u cbuser@sequenceiq.com:test123 -X GET -H "Content-Type:application/json" http://localhost:8080/credentials | jq .