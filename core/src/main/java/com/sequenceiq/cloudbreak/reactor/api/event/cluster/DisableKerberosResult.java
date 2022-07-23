package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class DisableKerberosResult extends ClusterPlatformResult<DisableKerberosRequest> implements FlowPayload {

    public DisableKerberosResult(DisableKerberosRequest request) {
        super(request);
    }

    @JsonCreator
    public DisableKerberosResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") DisableKerberosRequest request) {

        super(statusReason, errorDetails, request);
    }

    @Override
    public String toString() {
        return "DisableKerberosResult{" + super.toString() + "}";
    }
}
