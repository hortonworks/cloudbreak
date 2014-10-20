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
curl -iX POST -H "accept: application/x-www-form-urlencoded" -d 'credentials={"username":"wilbur@sequenceiq.com","password":"sargunaraj"}' "http://qa.uaa.sequenceiq.com/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" | grep Location | cut -d'=' -f 2 | cut -d'&' -f 1

#add blueprint
#curl -H "authorization:Bearer TOKEN..." -sX POST -H "Content-Type:application/json" -d '{"name":"singlenode-yarn","url":"https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/src/main/resources/blueprints/single-node-hdfs-yarn.bp"}' http://localhost:8090/user/blueprints | jq .

#get user events
curl -H "authorization:Bearer token...." -X GET "http://localhost:8090/events" | jq '.'
#generate user statistic
curl -H "authorization:Bearer token...." -X GET "http://localhost:8090/usages/generate"

#stop(start) cluster
curl -X PUT -H "Content-Type:application/json" -H "Authorization: Bearer token..." -d '{"status":"STOPPED"}' http://localhost:8090/stacks/100/cluster | jq .
#stop(start) stack
curl -X PUT -H "Content-Type:application/json" -H "Authorization: Bearer token..." -d '{"status":"STOPPED"}' http://localhost:8090/stacks/100 | jq .


eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjNmMyOWEzNS1iZTQxLTQ1MGQtYmE2YS02NzJjMDdjODc2NWYiLCJzdWIiOiIxM2NhN2RhNC04MDMyLTQ2NDEtODllOC0yN2JlZjhjNDRiZmUiLCJzY29wZSI6WyJjbG91ZGJyZWFrLnRlbXBsYXRlcyIsImNsb3VkYnJlYWsuY3JlZGVudGlhbHMiLCJjbG91ZGJyZWFrLnN0YWNrcyIsInBhc3N3b3JkLndyaXRlIiwib3BlbmlkIiwiY2xvdWRicmVhay5ibHVlcHJpbnRzIl0sImNsaWVudF9pZCI6ImNsb3VkYnJlYWtfc2hlbGwiLCJjaWQiOiJjbG91ZGJyZWFrX3NoZWxsIiwidXNlcl9pZCI6IjEzY2E3ZGE0LTgwMzItNDY0MS04OWU4LTI3YmVmOGM0NGJmZSIsInVzZXJfbmFtZSI6IndpbGJ1ckBzZXF1ZW5jZWlxLmNvbSIsImVtYWlsIjoid2lsYnVyQHNlcXVlbmNlaXEuY29tIiwiaWF0IjoxNDEzNTMzOTM2LCJleHAiOjE0MTM1NzcxMzYsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC91YWEvb2F1dGgvdG9rZW4iLCJhdWQiOlsiY2xvdWRicmVhayIsIm9wZW5pZCIsInBhc3N3b3JkIl19.iEDO8NIYWN2EjAD_OUzxphr468IhOgmT0pxdqmlfglg
