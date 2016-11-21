package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterStartPollingRequest extends ClusterPlatformRequest {

    private final Integer requestId;

    public ClusterStartPollingRequest(Long stackId, Integer requestId) {
        super(stackId);
        this.requestId = requestId;
    }

    public Integer getRequestId() {
        return requestId;
    }
}
