package com.sequenceiq.datalake.flow.cert.renew.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCertRenewalFailedEvent extends SdxEvent {

    private final String failureReason;

    public SdxCertRenewalFailedEvent(Long sdxId, String userId, String failureReason) {
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
