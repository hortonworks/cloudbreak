package com.sequenceiq.environment.environment.verification;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponse;

public class CDPServicePolicyVerification {

    private Set<CDPServicePolicyVerificationResponse> results;

    public CDPServicePolicyVerification(Set<CDPServicePolicyVerificationResponse> results) {
        this.results = results;
    }

    public Set<CDPServicePolicyVerificationResponse> getResults() {
        return results;
    }
}
