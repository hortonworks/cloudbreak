package com.sequenceiq.datalake.flow.certrotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RotateCertificateStackRequest extends SdxEvent {

    @JsonCreator
    public RotateCertificateStackRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static RotateCertificateStackRequest from(SdxContext context) {
        return new RotateCertificateStackRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return RotateCertificateStackRequest.class.getSimpleName();
    }
}

