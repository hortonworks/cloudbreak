#!/bin/bash
: ${ULU_ZIP:=oauth}

ULU_URL=https://github.com/sequenceiq/uluwatu/archive/$ULU_ZIP.zip

if [ -z "$ULU_CLOUDBREAK_ADDRESS" ]; then
  echo ULU_CLOUDBREAK_ADDRESS must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$ULU_IDENTITY_ADDRESS" ]; then
  echo ULU_IDENTITY_ADDRESS must be set;
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

if [ -z "$ULU_OAUTH_REDIRECT_URI" ]; then
  echo ULU_OAUTH_REDIRECT_URI must be set;
  MISSING_ENV_VARS=true;
fi

if [ $MISSING_ENV_VARS ]; then
  exit 1;
fi

cd /uluwatu && curl -LO $ULU_URL && unzip /uluwatu/$ULU_ZIP && cd /uluwatu/uluwatu-$ULU_ZIP && npm install && node server.js
