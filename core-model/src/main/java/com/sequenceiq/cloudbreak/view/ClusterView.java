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

    boolean isRangerRmsEnabled();

    CertExpirationState getCertExpirationState();

    String getCertExpirationDetails();

    String getEnvironmentCrn();

    String getProxyConfigCrn();

    String getVariant();

    String getUptime();

    ExecutorType getExecutorType();

    Secret getExtendedBlueprintTextSecret();

    Secret getAttributesSecret();

    Json getCustomContainerDefinition();

    Secret getDpClusterManagerUserSecret();

    Secret getDpClusterManagerPasswordSecret();

    Secret getCloudbreakClusterManagerUserSecretObject();

    Secret getCloudbreakClusterManagerPasswordSecretObject();

    Secret getCloudbreakClusterManagerMonitoringUserSecret();

    Secret getCloudbreakClusterManagerMonitoringPasswordSecret();

    Secret getCdpNodeStatusMonitorPasswordSecret();

    Secret getDatabusCredentialSecret();

    Secret getMonitoringCredentialSecret();

    Secret getKeyStorePwdSecret();

    Secret getTrustStorePwdSecret();

    Secret getPasswordSecret();

    Secret getUserNameSecret();

    FileSystem getFileSystem();

    FileSystem getAdditionalFileSystem();

    // getConfigurations() can cause lazy init, need to re-fetch with crn
    CustomConfigurations getCustomConfigurations();

    Boolean getEmbeddedDatabaseOnAttachedDisk();

    String getDbSslRootCertBundle();

    Boolean getDbSslEnabled();

    String getEncryptionProfileCrn();

    default String getPassword() {
        return getIfNotNull(getPasswordSecret(), Secret::getRaw);
    }

    default String getExtendedBlueprintText() {
        return getIfNotNull(getExtendedBlueprintTextSecret(), Secret::getRaw);
    }

    default String getAttributes() {
        return getIfNotNull(getAttributesSecret(), Secret::getRaw);
    }

    default String getCloudbreakClusterManagerUser() {
        return getIfNotNull(getCloudbreakClusterManagerUserSecretObject(), Secret::getRaw);
    }

    default String getCloudbreakClusterManagerPassword() {
        return getIfNotNull(getCloudbreakClusterManagerPasswordSecretObject(), Secret::getRaw);
    }

    default String getCloudbreakClusterManagerUserSecretPath() {
        return getIfNotNull(getCloudbreakClusterManagerUserSecretObject(), Secret::getSecret);
    }

    default String getCloudbreakClusterManagerPasswordSecretPath() {
        return getIfNotNull(getCloudbreakClusterManagerPasswordSecretObject(), Secret::getSecret);
    }

    default String getDpClusterManagerUser() {
        return getIfNotNull(getDpClusterManagerUserSecret(), Secret::getRaw);
    }

    default String getDpClusterManagerPassword() {
        return getIfNotNull(getDpClusterManagerPasswordSecret(), Secret::getRaw);
    }

    default String getDpClusterManagerUserSecretPath() {
        return getIfNotNull(getDpClusterManagerUserSecret(), Secret::getSecret);
    }

    default String getDpClusterManagerPasswordSecretPath() {
        return getIfNotNull(getDpClusterManagerPasswordSecret(), Secret::getSecret);
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

    default String getDatabusCredential() {
        return getIfNotNull(getDatabusCredentialSecret(), Secret::getRaw);
    }

    default String getMonitoringCredential() {
        return getIfNotNull(getMonitoringCredentialSecret(), Secret::getRaw);
    }

    default String getUserName() {
        return getIfNotNull(getUserNameSecret(), Secret::getRaw);
    }

    default String getKeyStorePwd() {
        String pwd = getIfNotNull(getKeyStorePwdSecret(), Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakClusterManagerPassword();
    }

    default String getTrustStorePwd() {
        String pwd = getIfNotNull(getTrustStorePwdSecret(), Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakClusterManagerPassword();
    }

    default boolean hasExternalDatabase() {
        return isNotEmpty(getDatabaseServerCrn());
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
