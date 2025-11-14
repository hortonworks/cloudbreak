#!/usr/bin/env bash

set -x

OUTPUT_FILE=/opt/salt-check-result.json

source activate_salt_env
salt-run manage.status --output=json | jq '.timestamp = (now | floor)' > ${OUTPUT_FILE}