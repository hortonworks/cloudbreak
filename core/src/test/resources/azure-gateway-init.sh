#!/bin/bash
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

find /usr/lib -name "os_family.json*" | xargs sed -i 's/amazon2015/amazon2016/g'

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log