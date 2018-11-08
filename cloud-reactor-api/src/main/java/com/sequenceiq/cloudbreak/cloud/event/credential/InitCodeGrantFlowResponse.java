package com.sequenceiq.cloudbreak.cloud.event.credential;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class InitCodeGrantFlowResponse extends CloudPlatformResult<InitCodeGrantFlowRequest> {
    private Map<String, String> codeGrantFlowInitParams;

    public InitCodeGrantFlowResponse(InitCodeGrantFlowRequest request, Map<String, String> codeGrantFlowInitParams) {
        super(request);
        this.codeGrantFlowInitParams = codeGrantFlowInitParams;
    }

    public InitCodeGrantFlowResponse(String statusReason, Exception errorDetails, InitCodeGrantFlowRequest request) {
        super(statusReason, errorDetails, request);
    }

    public Map<String, String> getCodeGrantFlowInitParams() {
        return codeGrantFlowInitParams;
    }
}
