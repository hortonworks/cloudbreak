package com.sequenceiq.thunderhead.controller.remotecluster.domain;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MockRemoteEnvironmentResponses {

    @JsonProperty
    private Set<MockRemoteEnvironmentResponse> environments = new HashSet<>();

    public Set<MockRemoteEnvironmentResponse> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<MockRemoteEnvironmentResponse> environments) {
        this.environments = environments;
    }

    @Override
    public String toString() {
        return "RemoteEnvironmentResponses{" +
                "environments=" + environments +
                '}';
    }
}
