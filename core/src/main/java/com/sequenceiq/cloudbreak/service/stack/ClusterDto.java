package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.common.api.type.CertExpirationState;

public interface ClusterDto {

    Long getId();
    Long getCreationFinished();
    Long getUpSince();
    boolean getAutoTlsEnabled();
    String getFqdn();
    String getClusterManagerIp();
    String getName();
    String getDescription();
    String getDatabaseServerCrn();
    boolean isRangerRazEnabled();
    CertExpirationState getCertExpirationState();
    Secret getEnvironmentCrn();
    String getProxyConfigCrn();
    String getUptime();
    Secret getExtendedBlueprintText();
    Secret getAttributes();
    Json getCustomContainerDefinition();
    Secret getDpAmbariUser();
    Secret getDpAmbariPassword();
    FileSystem getFileSystem();
    FileSystem getAdditionalFileSystem();
    CustomConfigurations getCustomConfigurations();
}
