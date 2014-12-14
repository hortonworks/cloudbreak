#!/bin/bash
: ${ULU_ZIP:=master}

export ULU_URL=https://github.com/sequenceiq/uluwatu/archive/$ULU_ZIP.zip
export ULU_SERVER_PORT=3000
export ULU_CLOUDBREAK_ADDRESS=http://localhost:8080
export ULU_IDENTITY_ADDRESS=http://qa.uaa.sequenceiq.com
export ULU_OAUTH_CLIENT_ID=uluwatu-dev
export ULU_IDENTITY_PORT=80
export ULU_OAUTH_CLIENT_SECRET=f2b9f54c-e8e4-4da7-b5ce-276db040ed6c
export ULU_OAUTH_REDIRECT_URI=http://localhost:3000/authorize
export ULU_SULTANS_ADDRESS=http://qa.accounts.sequenceiq.com
export ULU_HOST_ADDRESS=http://localhost:3000
 npm install && node server.js
