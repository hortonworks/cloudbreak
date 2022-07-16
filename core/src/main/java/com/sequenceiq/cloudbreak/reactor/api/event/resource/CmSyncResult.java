package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class CmSyncResult extends ClusterPlatformResult<CmSyncRequest> implements FlowPayload {

    private final String result;

    public CmSyncResult(CmSyncRequest request, String result) {
        super(request);
        this.result = result;
    }

    @JsonCreator
    public CmSyncResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") CmSyncRequest request) {
        super(statusReason, errorDetails, request);
        this.result = null;
    }

    public String getResult() {
        return result;
    }
}
