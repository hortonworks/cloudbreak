#!/bin/bash

if [ -z "$SEQ_REG_PORT" ]; then
  echo SEQ_REG_PORT must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$UAA_HOST" ]; then
  echo UAA_HOST must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$UAA_PORT" ]; then
  echo UAA_PORT must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$UR_SMTP_SENDER_HOST" ]; then
  echo UR_SMTP_SENDER_HOST must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$UR_SMTP_SENDER_PORT" ]; then
  echo UR_SMTP_SENDER_PORT must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$UR_SMTP_SENDER_USERNAME" ]; then
  echo UR_SMTP_SENDER_USERNAME must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$UR_SMTP_SENDER_PASSWORD" ]; then
  echo UR_SMTP_SENDER_PASSWORD must be set;
  MISSING_ENV_VARS=true;
fi

if [ -z "$UR_SMTP_SENDER_FROM" ]; then
  echo UR_SMTP_SENDER_FROM must be set;
  MISSING_ENV_VARS=true;
fi

if [ $MISSING_ENV_VARS ]; then
  exit 1;
fi

npm install && node main
