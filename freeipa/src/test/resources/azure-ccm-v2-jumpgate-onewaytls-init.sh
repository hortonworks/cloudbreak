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
export IS_PROXY_ENABLED=false
export IS_CCM_ENABLED=false
export IS_CCM_V2_ENABLED=true
export CCM_V2_INVERTING_PROXY_CERTIFICATE="invertingProxyCertificate"
export CCM_V2_INVERTING_PROXY_HOST="invertingProxyHost"
export CCM_V2_AGENT_CERTIFICATE="agentCertificate"
export CCM_V2_AGENT_ENCIPHERED_KEY="agentEncipheredPrivateKey"
export CCM_V2_AGENT_KEY_ID="agentKeyId"
export CCM_V2_AGENT_CRN="agentCrn"
export CCM_V2_AGENT_BACKEND_ID_PREFIX="agentCrn-"
export IS_CCM_V2_JUMPGATE_ENABLED=true
export CCM_V2_AGENT_ACCESS_KEY_ID="agentMachineUserAccessKeyId"
export CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY="agentMachineUserEncipheredAccessKey"
export CCM_V2_AGENT_HMAC_KEY="hmacKey"
export CCM_V2_IV="initialisationVector"
export CCM_V2_AGENT_HMAC_FOR_PRIVATE_KEY="hmacForPrivateKey"

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

date >> /tmp/time.txt

chmod o-rwx /var/log/user-data.log
chmod o-rwx /cdp/bin/*
chmod o-rwx -R /etc/certs/ || :
