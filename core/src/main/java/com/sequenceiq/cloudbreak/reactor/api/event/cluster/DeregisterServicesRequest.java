package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.AbstractClusterTerminationRequest;

public class DeregisterServicesRequest extends AbstractClusterTerminationRequest {

    @JsonCreator
    public DeregisterServicesRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("forced") boolean forced) {
        super(stackId, forced);
    }

    @Override
    public String toString() {
        return "DeregisterServicesRequest{" + super.toString() + "}";
    }
}
