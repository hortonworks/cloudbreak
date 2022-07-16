package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStartPillarConfigUpdateResult extends ClusterPlatformResult<ClusterStartPillarConfigUpdateRequest> implements FlowPayload {

    @JsonCreator
    public ClusterStartPillarConfigUpdateResult(
            @JsonProperty("request") ClusterStartPillarConfigUpdateRequest request) {
        super(request);
    }
}
