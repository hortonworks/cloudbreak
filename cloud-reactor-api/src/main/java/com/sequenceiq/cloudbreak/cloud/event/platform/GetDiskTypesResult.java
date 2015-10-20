package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;

public class GetDiskTypesResult extends CloudPlatformResult {
    private PlatformDisks platformDisks;

    public GetDiskTypesResult(CloudPlatformRequest<?> request, PlatformDisks platformDisks) {
        super(request);
        this.platformDisks = platformDisks;
    }

    public GetDiskTypesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public PlatformDisks getPlatformDisks() {
        return platformDisks;
    }
}
