#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -x

export CLOUD_PLATFORM="AZURE"
export START_LABEL=98
export PLATFORM_DISK_PREFIX=sd
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=false
export TMP_SSH_KEY="#NOT_USER_ANYMORE_BUT_KEEP_FOR_BACKWARD_COMPATIBILITY"
export SSH_USER=cloudbreak
export SALT_BOOT_PASSWORD=pass
export SALT_BOOT_SIGN_KEY=cHJpdi1rZXk=
export CB_CERT=cert
export IS_PROXY_ENABLED=false
export IS_CCM_ENABLED=false

date >> /tmp/time.txt

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log