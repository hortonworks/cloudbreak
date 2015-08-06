package com.sequenceiq.cloudbreak.cloud.event.setup;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class SetupResult extends CloudPlatformResult {

    private Map<String, Object> setupProperties;

    public SetupResult(CloudPlatformRequest<?> request, Map<String, Object> setupProperties) {
        super(request);
        this.setupProperties = setupProperties;
    }

    public SetupResult(Exception errorDetails, CloudPlatformRequest<?> request) {
        this(errorDetails.getMessage(), errorDetails, request);
    }

    public SetupResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public Map<String, Object> getSetupProperties() {
        return setupProperties;
    }
}
