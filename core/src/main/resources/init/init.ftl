#!/bin/bash

## logging
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

set -ex

export ENVIRONMENT_CRN="${environmentCrn}"
export CLOUD_PLATFORM="${cloudPlatform}"
export START_LABEL=${platformDiskStartLabel}
export PLATFORM_DISK_PREFIX=${platformDiskPrefix}
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=${gateway?c}
export TMP_SSH_KEY="${tmpSshKey}"
export SSH_USER=${sshUser}
<#if secretEncryptionEnabled!false>
###SECRETS-START
</#if>
export SALT_BOOT_PASSWORD=${saltBootPassword}
export SALT_BOOT_SIGN_KEY=${signaturePublicKey}
export CB_CERT=${cbCert}
<#if proxyEnabled!false>
export IS_PROXY_ENABLED=true
export PROXY_HOST=${proxyHost}
export PROXY_PORT=${proxyPort}
export PROXY_PROTOCOL=${proxyProtocol}
<#if proxyUser??>
export PROXY_USER="${proxyUser}"
<#if proxyPassword??>
export PROXY_PASSWORD="${proxyPassword}"
</#if>
</#if>
<#if proxyNoProxyHosts??>
export PROXY_NO_PROXY_HOSTS="${proxyNoProxyHosts}"
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
<#if ccmV2Enabled!false>
export IS_CCM_V2_ENABLED=true
<#if ccmV2InvertingProxyCertificate?has_content>
export CCM_V2_INVERTING_PROXY_CERTIFICATE="${ccmV2InvertingProxyCertificate}"
</#if>
<#if ccmV2InvertingProxyHost?has_content>
export CCM_V2_INVERTING_PROXY_HOST="${ccmV2InvertingProxyHost}"
</#if>
<#if ccmV2AgentCertificate?has_content>
export CCM_V2_AGENT_CERTIFICATE="${ccmV2AgentCertificate}"
</#if>
<#if ccmV2AgentEncipheredKey?has_content>
export CCM_V2_AGENT_ENCIPHERED_KEY="${ccmV2AgentEncipheredKey}"
</#if>
<#if ccmV2AgentKeyId?has_content>
export CCM_V2_AGENT_KEY_ID="${ccmV2AgentKeyId}"
</#if>
<#if ccmV2AgentCrn?has_content>
export CCM_V2_AGENT_CRN="${ccmV2AgentCrn}"
</#if>
<#if ccmV2AgentBackendIdPrefix?has_content>
export CCM_V2_AGENT_BACKEND_ID_PREFIX="${ccmV2AgentBackendIdPrefix}"
</#if>
<#else>
export IS_CCM_V2_ENABLED=false
</#if>
<#if ccmV2JumpgateEnabled!false>
export IS_CCM_V2_JUMPGATE_ENABLED=true
<#else>
export IS_CCM_V2_JUMPGATE_ENABLED=false
</#if>
<#if secretEncryptionEnabled!false>
###SECRETS-END
export SECRET_ENCRYPTION_ENABLED=true
export SECRET_ENCRYPTION_KEY_SOURCE="${secretEncryptionKeySource}"
</#if>

${customUserData}

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

chmod o-rwx /var/log/user-data.log
chmod o-rwx /cdp/bin/*
chmod o-rwx -R /etc/certs/ || :
