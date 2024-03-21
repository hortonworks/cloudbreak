package com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteEnvironmentResponses {

    @JsonProperty
    private Set<RemoteEnvironmentResponse> environments = new HashSet<>();

    public Set<RemoteEnvironmentResponse> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<RemoteEnvironmentResponse> environments) {
        this.environments = environments;
    }

    @Override
    public String toString() {
        return "RemoteEnvironmentResponses{" +
                "environments=" + environments +
                '}';
    }
}
