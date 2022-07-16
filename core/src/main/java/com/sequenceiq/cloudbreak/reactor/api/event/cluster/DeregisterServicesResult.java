package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class DeregisterServicesResult extends ClusterPlatformResult<DeregisterServicesRequest> implements FlowPayload {

    public DeregisterServicesResult(DeregisterServicesRequest request) {
        super(request);
    }

    @JsonCreator
    public DeregisterServicesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") DeregisterServicesRequest request) {
        super(statusReason, errorDetails, request);
    }

    @Override
    public String toString() {
        return "DisableKerberosResult{" + super.toString() + "}";
    }
}
