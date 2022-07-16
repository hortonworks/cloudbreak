package com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterServicesRestartRequest extends ClusterPlatformRequest {
    @JsonCreator
    public ClusterServicesRestartRequest(
            @JsonProperty("stackId") Long stackId) {
        super(stackId);
    }
}
