package com.sequenceiq.datalake.flow.cert.renew.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartCertRenewalEvent extends SdxEvent {

    private final boolean internal;

    public SdxStartCertRenewalEvent(Long sdxId, String userId) {
        super(sdxId, userId);
        internal = false;
    }

    @JsonCreator
    public SdxStartCertRenewalEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("internal") boolean internal) {
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
