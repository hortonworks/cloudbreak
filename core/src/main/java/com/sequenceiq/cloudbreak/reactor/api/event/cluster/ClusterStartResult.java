package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStartResult extends ClusterPlatformResult<ClusterStartRequest> implements FlowPayload {

    private final Integer requestId;

    public ClusterStartResult(ClusterStartRequest request, int requestId) {
        super(request);
        this.requestId = requestId;
    }

    @JsonCreator
    public ClusterStartResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterStartRequest request) {
        super(statusReason, errorDetails, request);
        requestId = null;
    }

    public Integer getRequestId() {
        return requestId;
    }
}
