package com.sequenceiq.datalake.flow.cert.renew.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartCertRenewalEvent extends SdxEvent {

    public SdxStartCertRenewalEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartCertRenewalEvent.class, other);
    }
}
