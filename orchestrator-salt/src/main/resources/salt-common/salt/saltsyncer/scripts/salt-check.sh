#!/usr/bin/env bash

set -x
set -o pipefail

OUTPUT_FILE=/opt/salt-check-result.json
# Written next to the result file and moved over it, so the status checker never reads a truncated file
TMP_FILE=$(mktemp ${OUTPUT_FILE}.XXXXXX) || exit 1
trap 'rm -f ${TMP_FILE}' EXIT

source activate_salt_env
salt-run manage.status --output=json | jq '.timestamp = (now | floor)' > ${TMP_FILE} && [ -s ${TMP_FILE} ] && chmod 644 ${TMP_FILE} && mv -f ${TMP_FILE} ${OUTPUT_FILE}
