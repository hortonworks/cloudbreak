#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -x

# warming up yum in the background
# yum -y makecache &

export CLOUD_PLATFORM="${cloudPlatform}"
export START_LABEL=${platformDiskStartLabel}
export PLATFORM_DISK_PREFIX=${platformDiskPrefix}
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=${gateway?c}
export TMP_SSH_KEY="${tmpSshKey}"
export PUBLIC_SSH_KEY="${publicSshKey}"
export RELOCATE_DOCKER=${relocateDocker?c}
export SSH_USER=${sshUser}

${customUserData}

curl -Lo /usr/sbin/salt-bootstrap https://dl.dropboxusercontent.com/s/hpt0496ay2o6904/salt-bootstrap
chmod +x /usr/sbin/salt-bootstrap

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log
