package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterSyncRequest extends ClusterPlatformRequest {
    @JsonCreator
    public ClusterSyncRequest(
            @JsonProperty("stackId") Long stackId) {
        super(stackId);
    }
}
