package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.Locale;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class GetDefaultPlatformDatabaseCapabilityRequest extends CloudPlatformRequest<GetDefaultPlatformDatabaseCapabilityResult> {

    private final String platform;

    public GetDefaultPlatformDatabaseCapabilityRequest(String platform) {
        super(null, null);
        this.platform = platform.toUpperCase(Locale.ROOT);
    }

    public String getPlatform() {
        return platform;
    }

    @Override
    public String toString() {
        return "GetDefaultPlatformDatabaseCapabilityRequest{}";
    }

}
