package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterStartPollingRequest extends ClusterPlatformRequest implements FlowPayload {

    private final Integer requestId;

    @JsonCreator
    public ClusterStartPollingRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("requestId") Integer requestId) {
        super(stackId);
        this.requestId = requestId;
    }

    public Integer getRequestId() {
        return requestId;
    }
}
