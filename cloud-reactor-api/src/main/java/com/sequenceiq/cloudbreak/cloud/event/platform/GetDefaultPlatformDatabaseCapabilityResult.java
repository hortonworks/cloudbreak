package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.DefaultPlatformDatabaseCapabilities;

public class GetDefaultPlatformDatabaseCapabilityResult extends CloudPlatformResult {

    private DefaultPlatformDatabaseCapabilities defaultPlatformDatabaseCapabilities;

    public GetDefaultPlatformDatabaseCapabilityResult(DefaultPlatformDatabaseCapabilities defaultPlatformDatabaseCapabilities) {
        super(null);
        this.defaultPlatformDatabaseCapabilities = defaultPlatformDatabaseCapabilities;
    }

    public GetDefaultPlatformDatabaseCapabilityResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public DefaultPlatformDatabaseCapabilities getDefaultPlatformDatabaseCapabilities() {
        return defaultPlatformDatabaseCapabilities;
    }
}
