#!/bin/bash
echo "Testing events ..."

: ${API_PORT:=8080}
: ${API_HOST:=vmati.cloudbreak.com}
: ${JSONDIR:=json/invite}
: ${ADMIN_EMAIL:=laszlo.puskas@sequenceiq.com}
: ${ADMIN_PASS:=cloudbreak}

# curl onfiguration for shorten the lines
cat << EOF >curl_config_admin_user
-S
-H "Content-Type: application/json"
--user $ADMIN_EMAIL:$ADMIN_PASS
EOF

# Invite a user
echo "Retrieve events ..."
# curl --config curl_config_admin_user  -X GET "http://$API_HOST:$API_PORT/events"

# User Usages ...
curl --config curl_config_admin_user  -X GET "http://$API_HOST:$API_PORT/user/usages"


