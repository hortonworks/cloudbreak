package com.sequenceiq.datalake.flow.certrotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class RotateCertificateFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public RotateCertificateFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static RotateCertificateFailedEvent from(SdxEvent event, Exception exception) {
        return new RotateCertificateFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return RotateCertificateFailedEvent.class.getSimpleName();
    }
}
