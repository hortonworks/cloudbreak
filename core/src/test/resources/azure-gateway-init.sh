#!/bin/bash
set -x

CLOUD_PLATFORM="AZURE"
START_LABEL=98
PLATFORM_DISK_PREFIX=sd
IS_GATEWAY=true
TMP_SSH_KEY="ssh-rsa test"
PUBLIC_SSH_KEY="ssh-rsa public"
RELOCATE_DOCKER=true

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log