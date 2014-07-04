#!/bin/bash
echo "Testing user registration ..."
API_PORT=8080
JSONDIR=json

curl --config curl_config http://localhost:$API_PORT/users --data @${JSONDIR}/user.json | jq '.'

#curl -X POST -H "Content-Type: application/json"  http://localhost:8080/users/confirm/22542cbe33b675e394b5800eaf837648 | jq '.'

