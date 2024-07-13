package com.sequenceiq.datalake.flow.certrotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RotateCertificateStackEvent extends SdxEvent {

    @JsonCreator
    public RotateCertificateStackEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(selector, sdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(RotateCertificateStackEvent.class, other);
    }
}
