package com.sequenceiq.datalake.flow.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent;

public class SdxEnableEncryptionProfileTriggerEvent extends SdxEvent {

    private final String encryptionProfileCrn;

    @JsonCreator
    public SdxEnableEncryptionProfileTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("encryptionProfileCrn") String encryptionProfileCrn) {
        super(selector, sdxId, userId);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    public static SdxEnableEncryptionProfileTriggerEvent from(Long sdxId, String userId, String encryptionProfileCrn) {
        return new SdxEnableEncryptionProfileTriggerEvent(
                SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_EVENT.event(),
                sdxId,
                userId,
                encryptionProfileCrn);
    }

    public String getEncryptionProfileCrn() {
        return encryptionProfileCrn;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxEnableEncryptionProfileTriggerEvent.class, other);
    }

    @Override
    public String toString() {
        return "SdxEnableEncryptionProfileTriggerEvent{" +
                "encryptionProfileCrn='" + encryptionProfileCrn + '\'' +
                "} " + super.toString();
    }
}
