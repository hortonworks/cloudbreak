#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -x

export CLOUD_PLATFORM="${cloudPlatform}"
export START_LABEL=${platformDiskStartLabel}
export PLATFORM_DISK_PREFIX=${platformDiskPrefix}
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=${gateway?c}
export TMP_SSH_KEY="dummy"
export SSH_USER=${sshUser}
export SALT_BOOT_PASSWORD=${saltBootPassword}
export SALT_BOOT_SIGN_KEY=${signaturePublicKey}
export CB_CERT=${cbCert}
<#if proxyEnabled!false>
export IS_PROXY_ENABLED=true
export PROXY_HOST=${proxyHost}
export PROXY_PORT=${proxyPort}
<#if proxyUser??>
export PROXY_USER="${proxyUser}"
<#if proxyPassword??>
export PROXY_PASSWORD="${proxyPassword}"
</#if>
</#if>
<#else>
export IS_PROXY_ENABLED=false
</#if>
<#if ccmEnabled!false>
export IS_CCM_ENABLED=true
export CCM_HOST=${ccmHost}
export CCM_SSH_PORT=${ccmSshPort?c}
export CCM_PUBLIC_KEY="${ccmPublicKey}"
<#if ccmTunnelInitiatorId??>
export CCM_TUNNEL_INITIATOR_ID="${ccmTunnelInitiatorId}"
</#if>
export CCM_KEY_ID="${ccmKeyId}"
export CCM_ENCIPHERED_PRIVATE_KEY="${ccmEncipheredPrivateKey}"
<#if ccmGatewayPort??>
export CCM_GATEWAY_PORT=${ccmGatewayPort?c}
</#if>
<#if ccmKnoxPort??>
export CCM_KNOX_PORT=${ccmKnoxPort?c}
</#if>
<#else>
export IS_CCM_ENABLED=false
</#if>

${customUserData}

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log