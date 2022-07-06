package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariServicesRequest extends StackEvent {

    private final boolean defaultClusterManagerAuth;

    @JsonCreator
    public StartAmbariServicesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("defaultClusterManagerAuth") boolean defaultClusterManagerAuth) {
        super(stackId);
        this.defaultClusterManagerAuth = defaultClusterManagerAuth;
    }

    public boolean isDefaultClusterManagerAuth() {
        return defaultClusterManagerAuth;
    }
}
