package com.sequenceiq.cloudbreak.cloud.event.credential;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class CredentialExperiencePolicyResult extends CloudPlatformResult {

    private Map<String, String> policies;

    public CredentialExperiencePolicyResult(Long resourceId, Map<String, String> policies) {
        super(resourceId);
        this.policies = policies;
    }

    public CredentialExperiencePolicyResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Map<String, String> getPolicies() {
        return policies;
    }
}
