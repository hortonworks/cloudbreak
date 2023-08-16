package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;

public class GetPlatformDatabaseCapabilityResult extends CloudPlatformResult {

    private PlatformDatabaseCapabilities platformDatabaseCapabilities;

    public GetPlatformDatabaseCapabilityResult(Long resourceId, PlatformDatabaseCapabilities platformDatabaseCapabilities) {
        super(resourceId);
        this.platformDatabaseCapabilities = platformDatabaseCapabilities;
    }

    public GetPlatformDatabaseCapabilityResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public PlatformDatabaseCapabilities getPlatformDatabaseCapabilities() {
        return platformDatabaseCapabilities;
    }
}
