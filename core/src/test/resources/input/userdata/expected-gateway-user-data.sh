#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -ex

export CLOUD_PLATFORM="${cloudPlatform}"
export START_LABEL=${platformDiskStartLabel}
export PLATFORM_DISK_PREFIX=${platformDiskPrefix}
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=${gateway?c}
export TMP_SSH_KEY="${tmpSshKey}"
export SSH_USER=${sshUser}
export SALT_BOOT_PASSWORD=${saltBootPassword}
export SALT_BOOT_SIGN_KEY=${signaturePublicKey}
export CB_CERT=${cbCert}

export IS_PROXY_ENABLED=false

export IS_CCM_V2_ENABLED=true
export IS_CCM_V2_JUMPGATE_ENABLED=false
export CDP_API_ENDPOINT_URL=""

${customUserData}

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

chmod o-rwx /var/log/user-data.log
chmod o-rwx /cdp/bin/*
chmod o-rwx -R /etc/certs/ || :
