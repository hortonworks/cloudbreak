package com.sequenceiq.datalake.flow.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class SdxEnableEncryptionProfileHandlerEvent extends SdxEvent {

    private final String encryptionProfileCrn;

    @JsonCreator
    public SdxEnableEncryptionProfileHandlerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("encryptionProfileCrn") String encryptionProfileCrn) {
        super(selector, sdxId, userId);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    public static SdxEnableEncryptionProfileHandlerEvent from(SdxContext context, String encryptionProfileCrn) {
        return new SdxEnableEncryptionProfileHandlerEvent(
                EventSelectorUtil.selector(SdxEnableEncryptionProfileHandlerEvent.class),
                context.getSdxId(), context.getUserId(), encryptionProfileCrn);
    }

    public String getEncryptionProfileCrn() {
        return encryptionProfileCrn;
    }

    @Override
    public String toString() {
        return "SdxEnableEncryptionProfileHandlerEvent{" +
                "encryptionProfileCrn='" + encryptionProfileCrn + '\'' +
                "} " + super.toString();
    }
}
