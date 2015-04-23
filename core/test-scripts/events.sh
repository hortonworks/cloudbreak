#!/bin/bash
echo "Testing events ..."

: ${API_PORT:=8080}
: ${API_HOST:=localhost}
: ${JSONDIR:=json/invite}
: ${ADMIN_EMAIL:=test@company.com}
: ${ADMIN_PASS:=pwd}

# curl onfiguration for shorten the lines
cat << EOF >curl_config_admin_user
-S
-H "Content-Type: application/json"
--user $ADMIN_EMAIL:$ADMIN_PASS
EOF

# Invite a user
echo "Retrieve events ..."
# curl --config curl_config_admin_user  -X GET "http://$API_HOST:$API_PORT/events" | jq '.'

# Generate usages
# curl --config curl_config_admin_user  -X GET "http://$API_HOST:$API_PORT/usages/generate"

# User Usages ...
curl --config curl_config_admin_user  -X GET "http://$API_HOST:$API_PORT/user/usages?vmtype=T2Micro" | jq '.'

#get token from uaa
TOKEN=$(curl -iX POST -H "accept: application/x-www-form-urlencoded" -d 'credentials={"username":"...","password":"..."}' "http://qa.uaa.sequenceiq.com/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" | grep Location | cut -d'=' -f 2 | cut -d'&' -f 1)

#get user events
curl -H "authorization:Bearer $TOKEN" -X GET "http://localhost:8090/events" | jq '.'
#generate user statistic
curl -H "authorization:Bearer $TOKEN" -X GET "http://localhost:8090/usages/generate"

#stop(start) cluster
curl -X PUT -H "Content-Type:application/json" -H "Authorization: Bearer token..." -d '{"status":"STOPPED"}' http://localhost:8090/stacks/100/cluster | jq .
#stop(start) stack
curl -X PUT -H "Content-Type:application/json" -H "Authorization: Bearer token..." -d '{"status":"STOPPED"}' http://localhost:8090/stacks/100 | jq .
