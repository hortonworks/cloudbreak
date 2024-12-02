package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.AbstractClusterTerminationRequest;

public class PrepareClusterTerminationRequest extends AbstractClusterTerminationRequest {

    @JsonCreator
    public PrepareClusterTerminationRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("forced") boolean forced) {
        super(stackId, forced);
    }
}
