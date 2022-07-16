package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscaleClusterManagerResult extends AbstractClusterScaleResult<UpscaleClusterManagerRequest> implements FlowPayload {

    public UpscaleClusterManagerResult(UpscaleClusterManagerRequest request) {
        super(request);
    }

    @JsonCreator
    public UpscaleClusterManagerResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") UpscaleClusterManagerRequest request) {
        super(statusReason, errorDetails, request);
    }
}
