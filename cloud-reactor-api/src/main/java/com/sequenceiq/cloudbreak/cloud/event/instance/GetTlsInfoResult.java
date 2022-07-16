package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class GetTlsInfoResult extends CloudPlatformResult implements FlowPayload {

    private final TlsInfo tlsInfo;

    public GetTlsInfoResult(Long resourceId, TlsInfo tlsInfo) {
        super(resourceId);
        this.tlsInfo = tlsInfo;
    }

    @JsonCreator
    public GetTlsInfoResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        this.tlsInfo = null;
    }

    public TlsInfo getTlsInfo() {
        return tlsInfo;
    }
}
