package com.sequenceiq.datalake.flow.cert.renew.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartCertRenewalEvent extends SdxEvent {

    private boolean internal;

    public SdxStartCertRenewalEvent(Long sdxId, String userId) {
        super(sdxId, userId);
        this.internal = false;
    }

    public SdxStartCertRenewalEvent(Long sdxId, String userId, boolean internal) {
        super(sdxId, userId);
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartCertRenewalEvent.class, other);
    }
}
