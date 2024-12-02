package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.AbstractClusterTerminationRequest;

public class ClusterTerminationRequest extends AbstractClusterTerminationRequest {
    private final Long clusterId;

    @JsonCreator
    public ClusterTerminationRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
        @JsonProperty("forced") boolean forced) {
        super(stackId, forced);
        this.clusterId = clusterId;
    }

    public Long getClusterId() {
        return clusterId;
    }
}
