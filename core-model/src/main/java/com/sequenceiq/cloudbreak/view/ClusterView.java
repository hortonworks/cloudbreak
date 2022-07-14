package com.sequenceiq.cloudbreak.view;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.common.api.type.CertExpirationState;

public interface ClusterView extends MdcContextInfoProvider {

    Long getId();

    Long getCreationFinished();

    Long getCreationStarted();

    Long getUpSince();

    Boolean getAutoTlsEnabled();

    String getFqdn();

    String getClusterManagerIp();

    String getName();

    String getDescription();

    String getDatabaseServerCrn();

    boolean isRangerRazEnabled();

    CertExpirationState getCertExpirationState();

    String getEnvironmentCrn();

    String getProxyConfigCrn();

    String getVariant();

    String getUptime();

    ExecutorType getExecutorType();

    Secret getExtendedBlueprintTextSecret();

    Secret getAttributesSecret();

    Json getCustomContainerDefinition();

    Secret getDpAmbariUserSecret();

    Secret getDpAmbariPasswordSecret();

    Secret getCloudbreakAmbariUserSecret();

    Secret getCloudbreakAmbariPasswordSecret();

    Secret getCloudbreakClusterManagerMonitoringUserSecret();

    Secret getCloudbreakClusterManagerMonitoringPasswordSecret();

    Secret getCdpNodeStatusMonitorUserSecret();

    Secret getCdpNodeStatusMonitorPasswordSecret();

    Secret getDatabusCredentialSecret();

    Secret getKeyStorePwdSecret();

    Secret getTrustStorePwdSecret();

    Secret getPasswordSecret();

    Secret getUserNameSecret();

    FileSystem getFileSystem();

    FileSystem getAdditionalFileSystem();

    CustomConfigurations getCustomConfigurations();

    Boolean getEmbeddedDatabaseOnAttachedDisk();

    default String getPassword() {
        return getIfNotNull(getPasswordSecret(), Secret::getRaw);
    }

    default String getExtendedBlueprintText() {
        return getIfNotNull(getExtendedBlueprintTextSecret(), Secret::getRaw);
    }

    default String getAttributes() {
        return getIfNotNull(getAttributesSecret(), Secret::getRaw);
    }

    default String getDpAmbariUser() {
        return getIfNotNull(getDpAmbariUserSecret(), Secret::getRaw);
    }

    default String getDpAmbariUserPath() {
        return getIfNotNull(getDpAmbariUserSecret(), Secret::getSecret);
    }

    default String getDpAmbariPassword() {
        return getIfNotNull(getDpAmbariPasswordSecret(), Secret::getRaw);
    }

    default String getDpAmbariPasswordPath() {
        return getIfNotNull(getDpAmbariPasswordSecret(), Secret::getSecret);
    }

    default String getCloudbreakAmbariUser() {
        return getIfNotNull(getCloudbreakAmbariUserSecret(), Secret::getRaw);
    }

    default String getCloudbreakAmbariPassword() {
        return getIfNotNull(getCloudbreakAmbariPasswordSecret(), Secret::getRaw);
    }

    default String getCloudbreakClusterManagerMonitoringUser() {
        return getIfNotNull(getCloudbreakClusterManagerMonitoringUserSecret(), Secret::getRaw);
    }

    default String getCloudbreakClusterManagerMonitoringPassword() {
        return getIfNotNull(getCloudbreakClusterManagerMonitoringPasswordSecret(), Secret::getRaw);
    }

    default String getCdpNodeStatusMonitorPassword() {
        return getIfNotNull(getCdpNodeStatusMonitorPasswordSecret(), Secret::getRaw);
    }

    default String getCdpNodeStatusMonitorUser() {
        return getIfNotNull(getCdpNodeStatusMonitorUserSecret(), Secret::getRaw);
    }

    default String getDatabusCredential() {
        return getIfNotNull(getDatabusCredentialSecret(), Secret::getRaw);
    }

    default String getUserName() {
        return getIfNotNull(getUserNameSecret(), Secret::getRaw);
    }

    default String getKeyStorePwd() {
        String pwd = getIfNotNull(getKeyStorePwdSecret(), Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakAmbariPassword();
    }

    default String getTrustStorePwd() {
        String pwd = getIfNotNull(getTrustStorePwdSecret(), Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakAmbariPassword();
    }

    @Override
    default String getResourceName() {
        return getName();
    }

    @Override
    default String getResourceType() {
        return "CLUSTER";
    }

    @Override
    default String getTenantName() {
        return null;
    }

    @Override
    default String getWorkspaceName() {
        return null;
    }
}
