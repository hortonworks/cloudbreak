package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterTerminationRequest extends ClusterPlatformRequest {
    private final Long clusterId;

    @JsonCreator
    public ClusterTerminationRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("clusterId") Long clusterId) {
        super(stackId);
        this.clusterId = clusterId;
    }

    public Long getClusterId() {
        return clusterId;
    }
}
