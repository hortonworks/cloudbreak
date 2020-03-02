package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class DeregisterServicesRequest extends ClusterPlatformRequest {

    public DeregisterServicesRequest(Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "DeregisterServicesRequest{" + super.toString() + "}";
    }
}
