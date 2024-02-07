package com.sequenceiq.datalake.flow.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxInstanceMetadataUpdateWaitSuccessEvent extends SdxEvent {

    @JsonCreator
    public SdxInstanceMetadataUpdateWaitSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String toString() {
        return "SdxInstanceMetadataUpdateWaitSuccessEvent{} " + super.toString();
    }

}