package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class ExtendHostMetadataResult extends AbstractClusterBootstrapResult<ExtendHostMetadataRequest> implements FlowPayload {
    public ExtendHostMetadataResult(ExtendHostMetadataRequest request) {
        super(request);
    }

    @JsonCreator
    public ExtendHostMetadataResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ExtendHostMetadataRequest request) {
        super(statusReason, errorDetails, request);
    }
}
