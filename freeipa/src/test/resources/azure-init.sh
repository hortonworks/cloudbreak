#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -ex

export ENVIRONMENT_CRN="environmentCrn"
export CDP_API_ENDPOINT_URL="endpointUrl"
export CLOUD_PLATFORM="AZURE"
export START_LABEL=98
export PLATFORM_DISK_PREFIX=sd
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=true
export TMP_SSH_KEY="dummy"
export SSH_USER=cloudbreak
export SALT_BOOT_PASSWORD=pass
export SALT_BOOT_SIGN_KEY=cHJpdi1rZXk=
export CB_CERT=cert
export IS_PROXY_ENABLED=false
export IS_CCM_ENABLED=false
export IS_CCM_V2_ENABLED=false
export IS_CCM_V2_JUMPGATE_ENABLED=false

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

date >> /tmp/time.txt

chmod o-rwx /var/log/user-data.log
chmod o-rwx /cdp/bin/*
chmod o-rwx -R /etc/certs/ || :
