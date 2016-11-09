package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class UnhealthyInstancesDetectionRequest extends ClusterPlatformRequest {

    public UnhealthyInstancesDetectionRequest(Long stackId) {
        super(stackId);
    }
}
