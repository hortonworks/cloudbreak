package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterCredentialChangeResult extends ClusterPlatformResult<ClusterCredentialChangeRequest> implements FlowPayload {
    public ClusterCredentialChangeResult(ClusterCredentialChangeRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterCredentialChangeResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterCredentialChangeRequest request) {
        super(statusReason, errorDetails, request);
    }
}
