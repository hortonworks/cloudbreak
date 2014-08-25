#!/bin/bash
echo "Testing user invite ..."

: ${API_PORT:=8080}
: ${API_HOST:=vmati.cloudbreak.com}
: ${JSONDIR:=json/invite}
: ${ADMIN_EMAIL:=admin@email.com}
: ${ADMIN_PASS:=cloudbreak}

# curl onfiguration for shorten the lines
cat << EOF >curl_config_admin_user
-v
-S
-H "Content-Type: application/json"
--user $ADMIN_EMAIL:$ADMIN_PASS
EOF

# Invite a user
INVITE_TOKEN=$(curl --config curl_config_admin_user http://$API_HOST:$API_PORT/admin/users/invite -X POST --data @$JSONDIR/account_user_invite.json)
echo "Invite token: $INVITE_TOKEN"

# Get invited user
echo "Invited user ..."
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/invite/$INVITE_TOKEN -X GET | jq '.'

# Confirming invitation
echo "Confirm upon invitation ..."
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/users/invite/$INVITE_TOKEN -X PUT --data @$JSONDIR/confirm_invite.json | jq '.'

USER_ID=21
# Role update
echo "Role update ..."
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/admin/users/$USER_ID -X PUT --data @$JSONDIR/role_update.json | jq '.'

# Status update
echo "Status update ..."
curl --config curl_config_admin_user http://$API_HOST:$API_PORT/admin/users/$USER_ID -X PUT --data @$JSONDIR/status_update.json | jq '.'
