#!/bin/bash
: ${SL_ADDRESS:?"Please set the SL_ADDRESS environment variable!"}
: ${SL_PORT:?"Please set the SL_PORT environment variable!"}
: ${SL_CLIENT_ID:?"Please set the SL_CLIENT_ID environment variable!"}
: ${SL_CLIENT_SECRET:?"Please set the SL_CLIENT_ID environment variable!"}
: ${SL_UAA_ADDRESS:?"Please set the SL_UAA_ADDRESS environment variable!"}
: ${SL_SMTP_SENDER_HOST:?"Please set the SL_SMTP_SENDER_HOST environment variable!"}
: ${SL_SMTP_SENDER_PORT:?"Please set the SL_SMTP_SENDER_PORT environment variable!"}
: ${SL_SMTP_SENDER_USERNAME:?"Please set the SL_SMTP_SENDER_USERNAME environment variable!"}
: ${SL_SMTP_SENDER_PASSWORD:?"Please set the SL_SMTP_SENDER_PASSWORD environment variable!"}
: ${SL_SMTP_SENDER_FROM:?"Please set the SL_SMTP_SENDER_FROM environment variable!"}
: ${SL_CB_ADDRESS:?"Please set the SL_ADDRESS environment variable!"}

npm install && node main