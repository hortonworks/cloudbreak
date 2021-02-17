package com.sequenceiq.cloudbreak.platform;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Component
public class ExternalDatabasePlatformConfig {

    @Inject
    private PlatformConfig platformConfig;

    public boolean isExternalDatabaseSupportedFor(CloudPlatform cloudPlatform) {
        return platformConfig.getSupportedExternalDatabasePlatforms().contains(cloudPlatform);
    }

    public boolean isPauseSupportedForExternalDatabase(CloudPlatform cloudPlatform) {
        return platformConfig.getDatabasePauseSupportedPlatforms().contains(cloudPlatform);
    }

    public boolean isSslEnforcementSupportedForForExternalDatabase(CloudPlatform cloudPlatform) {
        return platformConfig.getDatabaseSslEnforcementSupportedPlatforms().contains(cloudPlatform);
    }

    public Set<CloudPlatform> getSupportedExternalDatabasePlatforms() {
        return platformConfig.getSupportedExternalDatabasePlatforms();
    }
}
