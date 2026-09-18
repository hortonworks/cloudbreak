package com.sequenceiq.datalake.flow.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDisableEncryptionProfileFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public SdxDisableEncryptionProfileFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxDisableEncryptionProfileFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxDisableEncryptionProfileFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "SdxDisableEncryptionProfileFailedEvent{} " + super.toString();
    }
}
