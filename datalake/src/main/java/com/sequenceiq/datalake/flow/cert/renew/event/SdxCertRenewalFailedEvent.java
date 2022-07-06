package com.sequenceiq.datalake.flow.cert.renew.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCertRenewalFailedEvent extends SdxEvent {

    private final String failureReason;

    @JsonCreator
    public SdxCertRenewalFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("failureReason") String failureReason) {
        super(sdxId, userId);
        this.failureReason = failureReason;
    }

    public SdxCertRenewalFailedEvent(SdxEvent event, String failureReason) {
        super(event.getResourceId(), event.getUserId());
        this.failureReason = failureReason;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
