package com.sequenceiq.datalake.flow.certrotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RotateCertificateSuccessEvent extends SdxEvent {

    @JsonCreator
    public RotateCertificateSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RotateCertificateSuccessEvent.class.getSimpleName();
    }
}
