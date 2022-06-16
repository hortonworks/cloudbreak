package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariServicesRequest extends StackEvent {

    private final boolean defaultClusterManagerAuth;

    public StartAmbariServicesRequest(Long stackId, boolean defaultClusterManagerAuth) {
        super(stackId);
        this.defaultClusterManagerAuth = defaultClusterManagerAuth;
    }

    public boolean isDefaultClusterManagerAuth() {
        return defaultClusterManagerAuth;
    }
}