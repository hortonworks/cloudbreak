#!/bin/bash
set -x

# warming up yum in the background
yum -y install wget &

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

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

service cloudbreak-bootstrap stop
rm -rf /usr/sbin/cloudbreak-bootstrap
curl -Lo /usr/sbin/cloudbreak-bootstrap https://dl.dropboxusercontent.com/u/13919958/cloudbreak-bootstrap && chmod +x /usr/sbin/cloudbreak-bootstrap
systemctl daemon-reload
systemctl start cloudbreak-bootstrap
