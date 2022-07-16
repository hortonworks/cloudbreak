package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class MountDisksOnNewHostsResult extends AbstractClusterBootstrapResult<MountDisksOnNewHostsRequest> implements FlowPayload {
    public MountDisksOnNewHostsResult(MountDisksOnNewHostsRequest request) {
        super(request);
    }

    @JsonCreator
    public MountDisksOnNewHostsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") MountDisksOnNewHostsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
