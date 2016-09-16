#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -x

export CLOUD_PLATFORM="AZURE_RM"
export START_LABEL=98
export PLATFORM_DISK_PREFIX=sd
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=false
export TMP_SSH_KEY="ssh-rsa test"
export SIGN_KEY="ssh-rsa test"
export PUBLIC_SSH_KEY="ssh-rsa public"
export RELOCATE_DOCKER=true
export SSH_USER=cloudbreak

date >> /tmp/time.txt

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

mkdir -p /etc/systemd/system/salt-bootstrap.service.d
cat > /etc/systemd/system/salt-bootstrap.service.d/envs.conf << EOF
[Service]
Environment="SALTBOOT_USERNAME=cbadmin"
Environment="SALTBOOT_PASSWORD=pass"
EOF
systemctl daemon-reload