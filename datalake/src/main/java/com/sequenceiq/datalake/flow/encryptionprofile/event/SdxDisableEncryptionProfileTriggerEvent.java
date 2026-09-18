package com.sequenceiq.datalake.flow.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent;

public class SdxDisableEncryptionProfileTriggerEvent extends SdxEvent {

    @JsonCreator
    public SdxDisableEncryptionProfileTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(selector, sdxId, userId);
    }

    public static SdxDisableEncryptionProfileTriggerEvent from(Long sdxId, String userId) {
        return new SdxDisableEncryptionProfileTriggerEvent(
                SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_EVENT.event(),
                sdxId,
                userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxDisableEncryptionProfileTriggerEvent.class, other);
    }

    @Override
    public String toString() {
        return "SdxDisableEncryptionProfileTriggerEvent{} " + super.toString();
    }
}
