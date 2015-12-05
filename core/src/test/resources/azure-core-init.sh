#!/bin/bash
set -x

export CLOUD_PLATFORM="AZURE"
export START_LABEL=98
export PLATFORM_DISK_PREFIX=sd
export IS_GATEWAY=false
export TMP_SSH_KEY="ssh-rsa test"
export PUBLIC_SSH_KEY="ssh-rsa public"
export RELOCATE_DOCKER=true
export SSH_USER=cloudbreak

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log