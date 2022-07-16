package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterSyncResult extends ClusterPlatformResult<ClusterSyncRequest> implements FlowPayload {
    public ClusterSyncResult(ClusterSyncRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterSyncResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterSyncRequest request) {
        super(statusReason, errorDetails, request);
    }
}
