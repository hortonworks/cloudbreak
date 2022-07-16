package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class RegenerateKerberosKeytabsResult extends AbstractClusterScaleResult<RegenerateKerberosKeytabsRequest> implements FlowPayload {
    public RegenerateKerberosKeytabsResult(RegenerateKerberosKeytabsRequest request) {
        super(request);
    }

    @JsonCreator
    public RegenerateKerberosKeytabsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") RegenerateKerberosKeytabsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
