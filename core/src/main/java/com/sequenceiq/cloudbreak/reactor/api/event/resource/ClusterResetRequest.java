package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterResetRequest extends ClusterPlatformRequest {
    @JsonCreator
    public ClusterResetRequest(
            @JsonProperty("stackId") Long stackId) {
        super(stackId);
    }
}
