package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class DisableProxyAuthFeature implements Feature {
    @Override
    public boolean configure(FeatureContext context) {
        context.register(new DisableProxyAuthFilter());
        return true;
    }
}
