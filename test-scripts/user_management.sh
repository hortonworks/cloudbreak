#!/bin/bash
echo "Testing user invite ..."

: ${API_PORT:=8080}
: ${API_HOST:=vmati.cloudbreak.com}
: ${JSONDIR:=json/invite}
: ${ADMIN_EMAIL:=test@sequenceiq.com}
: ${ADMIN_PASS:=cloudbreak}

# curl onfiguration for shorten the lines
cat << EOF >curl_config_admin_user
-S
-H "Content-Type: application/json"
--user $ADMIN_EMAIL:$ADMIN_PASS
EOF

# Invite a user
echo "Invite a user ..."
echo "curl --config curl_config_admin_user  -X POST --data @$JSONDIR/account_user_invite.json "http://$API_HOST:$API_PORT/users/invite" "
INVITE_TOKEN=$(curl --config curl_config_admin_user  -X POST --data @$JSONDIR/account_user_invite.json "http://$API_HOST:$API_PORT/users/invite")
echo "Invite token: $INVITE_TOKEN"

# Get invited user
echo "Invited user ..."
echo "curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/invite/$INVITE_TOKEN -X GET | jq '.'"
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/invite/$INVITE_TOKEN -X GET | jq '.'

# Confirming invitation
echo "Confirm upon invitation ..."
echo "curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/invite/$INVITE_TOKEN -X PUT --data @$JSONDIR/confirm_invite.json | jq '.'"
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/invite/$INVITE_TOKEN -X PUT --data @$JSONDIR/confirm_invite.json | jq '.'

USER_ID=21
# Role update
echo "Role update ..."
echo "curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/$USER_ID -X PUT --data @$JSONDIR/role_update.json | jq '.'"
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/$USER_ID -X PUT --data @$JSONDIR/role_update.json | jq '.'

# Status update
echo "Status update ..."
echo "curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/$USER_ID -X PUT --data @$JSONDIR/status_update.json | jq '.'"
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/$USER_ID -X PUT --data @$JSONDIR/status_update.json | jq '.'
