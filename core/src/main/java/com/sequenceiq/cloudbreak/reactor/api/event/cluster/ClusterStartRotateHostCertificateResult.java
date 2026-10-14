package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStartRotateHostCertificateResult extends ClusterPlatformResult<ClusterStartRotateHostCertificateRequest> implements FlowPayload {

    public ClusterStartRotateHostCertificateResult(ClusterStartRotateHostCertificateRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterStartRotateHostCertificateResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterStartRotateHostCertificateRequest request) {
        super(statusReason, errorDetails, request);
    }
}
