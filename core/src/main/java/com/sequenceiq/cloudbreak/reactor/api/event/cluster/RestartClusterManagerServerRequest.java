package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartClusterManagerServerRequest extends StackEvent {
    private final boolean defaultClusterManagerAuth;

    private final String failureSelector;

    @JsonCreator
    public RestartClusterManagerServerRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("defaultClusterManagerAuth") boolean defaultClusterManagerAuth,
            @JsonProperty("failureSelector") String failureSelector) {
        super(stackId);
        this.defaultClusterManagerAuth = defaultClusterManagerAuth;
        this.failureSelector = failureSelector;
    }

    public boolean isDefaultClusterManagerAuth() {
        return defaultClusterManagerAuth;
    }

    public String getFailureSelector() {
        return failureSelector;
    }
}
