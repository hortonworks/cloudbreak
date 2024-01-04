package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

public class SetProxyTimeoutFeature implements Feature {

    private final Integer timeout;

    public SetProxyTimeoutFeature(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new SetProxyTimeoutFilter(timeout));
        return true;
    }
}
