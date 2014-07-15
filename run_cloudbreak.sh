#!/bin/bash
# the address where the app is hosted
: ${HOST_ADDR:?"Please set the HOST_ADDR environment variable!"}

# database settings
: ${DB_ENV_USER:?"Please set the DB_ENV_USER environment variable!"}
: ${DB_ENV_PASS:?"Please set the DB_ENV_PASS environment variable!"}
: ${DB_PORT_5432_TCP_ADDR:?"Please set the DB_PORT_5432_TCP_ADDR environment variable!"}
: ${DB_PORT_5432_TCP_PORT:?"Please set the DB_PORT_5432_TCP_PORT environment variable!"}
: ${HBM2DDL_STRATEGY:-"create"}

# SMTP properties
: ${MAIL_SENDER_USERNAME:?"Please set the MAIL_SENDER_USERNAME environment variable!"}
: ${MAIL_SENDER_PASSWORD:?"Please set the MAIL_SENDER_PASSWORD environment variable!"}
: ${MAIL_SENDER_HOST:?"Please set the MAIL_SENDER_HOST environment variable!"}
: ${MAIL_SENDER_PORT:?"Please set the MAIL_SENDER_PORT environment variable!"}
: ${MAIL_SENDER_FROM:?"Please set the MAIL_SENDER_FROM environment variable!"}
: ${AZURE_IMAGE_URI:="http://vmdepoteastus.blob.core.windows.net/linux-community-store/community-62091-a59dcdc1-d82d-4e76-9094-27b8c018a4a1-1.vhd"}
: ${BLUEPRINT_DEFAULTS:='lambda-architecture,multi-node-hdfs-yarn,single-node-hdfs-yarn'}

java -jar build/libs/cloudbreak-0.1-DEV-defaults.jar
