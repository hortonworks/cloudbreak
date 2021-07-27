package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Set;

public class CDPServicePolicyVerificationResponses {

    private Set<CDPServicePolicyVerificationResponse> results;

    public CDPServicePolicyVerificationResponses(Set<CDPServicePolicyVerificationResponse> results) {
        this.results = results;
    }

    public Set<CDPServicePolicyVerificationResponse> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "CDPServicePolicyVerificationResults{" +
                "results=" + results +
                '}';
    }
}
