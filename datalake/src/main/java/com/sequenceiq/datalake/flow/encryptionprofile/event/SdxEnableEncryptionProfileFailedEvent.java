package com.sequenceiq.datalake.flow.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxEnableEncryptionProfileFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public SdxEnableEncryptionProfileFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxEnableEncryptionProfileFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxEnableEncryptionProfileFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "SdxEnableEncryptionProfileFailedEvent{} " + super.toString();
    }
}
