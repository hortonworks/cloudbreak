#!/bin/bash
: ${UR_PORT:?"Please set the UR_PORT environment variable!"}
: ${UAA_HOST:?"Please set the UAA_HOST environment variable!"}
: ${UAA_PORT:?"Please set the UR_PORT environment variable!"}
: ${UR_SMTP_SENDER_HOST:?"Please set the UR_SMTP_SENDER_HOST environment variable!"}
: ${UR_SMTP_SENDER_PORT:?"Please set the UR_SMTP_SENDER_PORT environment variable!"}
: ${UR_SMTP_SENDER_USERNAME:?"Please set the UR_SMTP_SENDER_USERNAME environment variable!"}
: ${UR_SMTP_SENDER_PASSWORD:?"Please set the UR_SMTP_SENDER_PASSWORD environment variable!"}
: ${UR_SMTP_SENDER_FROM:?"Please set the UR_SMTP_SENDER_FROM environment variable!"}

npm install && node main