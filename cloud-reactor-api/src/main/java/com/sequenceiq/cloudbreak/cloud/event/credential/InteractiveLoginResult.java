package com.sequenceiq.cloudbreak.cloud.event.credential;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class InteractiveLoginResult extends CloudPlatformResult {

    private Map<String, String> parameters;

    public InteractiveLoginResult(Long resourceId, Map<String, String> parameters) {
        super(resourceId);
        this.parameters = parameters;
    }

    public InteractiveLoginResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
