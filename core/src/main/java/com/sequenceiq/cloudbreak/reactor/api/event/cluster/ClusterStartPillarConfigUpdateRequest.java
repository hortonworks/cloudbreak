package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterStartPillarConfigUpdateRequest extends ClusterPlatformRequest {
    @JsonCreator
    public ClusterStartPillarConfigUpdateRequest(
            @JsonProperty("stackId") Long stackId) {
        super(stackId);
    }
}
