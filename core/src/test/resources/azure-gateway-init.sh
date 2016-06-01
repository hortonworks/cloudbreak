#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -x

# warming up yum in the background
# yum -y makecache &

export CLOUD_PLATFORM="AZURE_RM"
export START_LABEL=98
export PLATFORM_DISK_PREFIX=sd
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=true
export TMP_SSH_KEY="ssh-rsa test"
export PUBLIC_SSH_KEY="ssh-rsa public"
export RELOCATE_DOCKER=true
export SSH_USER=cloudbreak

date >> /tmp/time.txt

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log
