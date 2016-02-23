#!/bin/bash

[[ "TRACE" ]] && set -x

if [ -z "$ULU_CLOUDBREAK_ADDRESS" ] && [ -z "$ULU_CLOUDBREAK_SERVICEID" ]; then
  echo ULU_CLOUDBREAK_ADDRESS or ULU_CLOUDBREAK_SERVICEID must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$ULU_IDENTITY_ADDRESS" ] && [ -z "$ULU_IDENTITY_SERVICEID" ]; then
  echo ULU_IDENTITY_ADDRESS or ULU_IDENTITY_SERVICEID must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$ULU_SULTANS_ADDRESS" ]; then
  echo ULU_SULTANS_ADDRESS must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$ULU_OAUTH_CLIENT_ID" ]; then
  echo ULU_OAUTH_CLIENT_ID must be set;
  ULU_OAUTH_CLIENT_ID=true;
fi

if [ -z "$ULU_OAUTH_CLIENT_SECRET" ]; then
  echo ULU_OAUTH_CLIENT_SECRET must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$ULU_HOST_ADDRESS" ]; then
  echo ULU_HOST_ADDRESS must be set;
  MISSING_ENV_VARS=true;
fi

if [ $MISSING_ENV_VARS ]; then
  exit 1;
fi

cd /uluwatu
node server.js
