package com.sequenceiq.cloudbreak.cloud.event.credential;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class InitCodeGrantFlowResponse extends CloudPlatformResult {
    private Map<String, String> codeGrantFlowInitParams;

    public InitCodeGrantFlowResponse(Long resourceId, Map<String, String> codeGrantFlowInitParams) {
        super(resourceId);
        this.codeGrantFlowInitParams = codeGrantFlowInitParams;
    }

    public InitCodeGrantFlowResponse(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Map<String, String> getCodeGrantFlowInitParams() {
        return codeGrantFlowInitParams;
    }
}
