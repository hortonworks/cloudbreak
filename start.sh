#!/bin/bash

: ${ULU_CLOUDBREAK_HOST:=$ULU_CB_PORT_8080_TCP_ADDR}
: ${ULU_CLOUDBREAK_PORT:=$ULU_CB_PORT_8080_TCP_PORT}


export ULU_SERVER_PORT=
export ULU_CLOUDBREAK_ADDRESS=
export ULU_IDENTITY_ADDRESS=
export ULU_OAUTH_CLIENT_ID=
export ULU_IDENTITY_POR=
export ULU_OAUTH_CLIENT_SECRET=
export ULU_OAUTH_REDIRECT_URI=



if [ -z "$ULU_OAUTH_CLIENT_ID" ]; then
  echo ULU_OAUTH_CLIENT_ID must be set;
  ULU_OAUTH_CLIENT_ID=true;
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

npm install
node server.js
