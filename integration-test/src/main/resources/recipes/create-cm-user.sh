#!/bin/bash
set -e

export USER_UUID=$(curl -s -u CM_USER:CM_PASSWORD -H 'Content-Type: application/json' -X GET http://localhost:7180/api/v31/users/cloudbreak | jq -r '.authRoles[].uuid')

curl -u CM_USER:CM_PASSWORD -i -H "Content-Type: application/json" -X POST -d '{"items":[{"name":"teszt","password":"Teszt123","authRoles":[{"uuid":"'"$USER_UUID"'","displayName":"Full Administrator"}]}]}' http://localhost:7180/api/v31/users
