package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartClusterManagerServerRequest extends StackEvent {
    private boolean defaultClusterManagerAuth;

    private String failureSelector;

    public RestartClusterManagerServerRequest(Long stackId, boolean defaultClusterManagerAuth, String failureSelector) {
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
