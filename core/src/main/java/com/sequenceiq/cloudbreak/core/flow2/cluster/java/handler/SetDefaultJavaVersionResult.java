package com.sequenceiq.cloudbreak.core.flow2.cluster.java.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class SetDefaultJavaVersionResult extends ClusterPlatformResult<SetDefaultJavaVersionRequest> implements FlowPayload {

    public SetDefaultJavaVersionResult(SetDefaultJavaVersionRequest request) {
        super(request);
    }

    @JsonCreator
    public SetDefaultJavaVersionResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") SetDefaultJavaVersionRequest request) {
        super(statusReason, errorDetails, request);
    }

}
