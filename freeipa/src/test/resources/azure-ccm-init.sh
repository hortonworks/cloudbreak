#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -ex

export ENVIRONMENT_CRN="environmentCrn"
export CDP_API_ENDPOINT_URL=""
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
export IS_PROXY_ENABLED=true
export PROXY_HOST=proxy.host
export PROXY_PORT=1234
export PROXY_PROTOCOL=https
export PROXY_USER="user"
export PROXY_PASSWORD="pwd"
export PROXY_NO_PROXY_HOSTS="noproxy.com"
export IS_CCM_ENABLED=true
export CCM_HOST=ccm.cloudera.com
export CCM_SSH_PORT=8990
export CCM_PUBLIC_KEY="W2NjbS5jbG91ZGVyYS5jb21dOjg5OTAgcHViLWtleQ=="
export CCM_TUNNEL_INITIATOR_ID="tunnel-id"
export CCM_KEY_ID="key-id"
export CCM_ENCIPHERED_PRIVATE_KEY="cHJpdmF0ZS1rZXk="
export CCM_GATEWAY_PORT=9443
export IS_CCM_V2_ENABLED=false
export IS_CCM_V2_JUMPGATE_ENABLED=false

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

date >> /tmp/time.txt

chmod o-rwx /var/log/user-data.log
chmod o-rwx /cdp/bin/*
chmod o-rwx -R /etc/certs/ || :
