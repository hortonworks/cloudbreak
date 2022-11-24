package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterDbCertRotationResult extends ClusterPlatformResult<ClusterDbCertRotationRequest> implements FlowPayload {

    public ClusterDbCertRotationResult(ClusterDbCertRotationRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterDbCertRotationResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterDbCertRotationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
