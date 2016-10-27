package com.sequenceiq.cloudbreak.cloud.event.credential;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class InteractiveLoginResult extends CloudPlatformResult<CloudPlatformRequest> {

    private Map<String, String> parameters;

    public InteractiveLoginResult(CloudPlatformRequest<InteractiveLoginResult> request, Map<String, String> parameters) {
        super(request);
        this.parameters = parameters;
    }

    public InteractiveLoginResult(String statusReason, Exception errorDetails, CloudPlatformRequest<InteractiveLoginResult> request) {
        super(statusReason, errorDetails, request);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
