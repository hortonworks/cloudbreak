#!/bin/bash
# the address where the app is hosted
: ${CB_HOST_ADDR:?"Please set the CB_HOST_ADDR environment variable!"}

# database settings
: ${CB_DB_ENV_USER:?"Please set the CB_DB_ENV_USER environment variable!"}
: ${CB_DB_ENV_PASS:?"Please set the CB_DB_ENV_PASS environment variable!"}
: ${CB_DB_PORT_5432_TCP_ADDR:?"Please set the CB_DB_PORT_5432_TCP_ADDR environment variable!"}
: ${CB_DB_PORT_5432_TCP_PORT:?"Please set the CB_DB_PORT_5432_TCP_PORT environment variable!"}
: ${CB_HBM2DDL_STRATEGY:="create"}

# SMTP properties
: ${CB_MAIL_SENDER_USERNAME:?"Please set the CB_MAIL_SENDER_USERNAME environment variable!"}
: ${CB_MAIL_SENDER_PASSWORD:?"Please set the CB_MAIL_SENDER_PASSWORD environment variable!"}
: ${CB_MAIL_SENDER_HOST:?"Please set the CB_MAIL_SENDER_HOST environment variable!"}
: ${CB_MAIL_SENDER_PORT:?"Please set the CB_MAIL_SENDER_PORT environment variable!"}
: ${CB_MAIL_SENDER_FROM:?"Please set the CB_MAIL_SENDER_FROM environment variable!"}

# Azure
: ${CB_AZURE_IMAGE_URI:="http://vmdepoteastus.blob.core.windows.net/linux-community-store/community-62091-a59dcdc1-d82d-4e76-9094-27b8c018a4a1-1.vhd"}

# Ambari
: ${CB_MANAGEMENT_CONTEXT_PATH:?"Please set the CB_MANAGEMENT_CONTEXT_PATH environment variable!"}
: ${CB_BLUEPRINT_DEFAULTS:="lambda-architecture,multi-node-hdfs-yarn,single-node-hdfs-yarn"}

# AWS related (optional) settings - not setting them causes AWS related operations to fail
: ${AWS_ACCESS_KEY_ID:?"Please set the AWS_ACCESS_KEY_ID environment variable!"}
: ${AWS_SECRET_KEY:?"Please set the AWS_SECRET_KEY environment variable!"}


java -jar build/libs/cloudbreak-0.1-DEV-renamed-properties.jar
