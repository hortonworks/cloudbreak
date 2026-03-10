package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ResetJvmParamsResult extends ClusterPlatformResult<ResetJvmParamsRequest> implements FlowPayload {

    public ResetJvmParamsResult(ResetJvmParamsRequest request) {
        super(request);
    }

    @JsonCreator
    public ResetJvmParamsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ResetJvmParamsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
