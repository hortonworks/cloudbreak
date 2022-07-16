package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterProxyReRegistrationResult extends ClusterPlatformResult<ClusterProxyReRegistrationRequest> implements FlowPayload {
    public ClusterProxyReRegistrationResult(ClusterProxyReRegistrationRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterProxyReRegistrationResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterProxyReRegistrationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
