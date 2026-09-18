package com.sequenceiq.datalake.flow.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class SdxDisableEncryptionProfileHandlerEvent extends SdxEvent {

    @JsonCreator
    public SdxDisableEncryptionProfileHandlerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(selector, sdxId, userId);
    }

    public static SdxDisableEncryptionProfileHandlerEvent from(SdxContext context) {
        return new SdxDisableEncryptionProfileHandlerEvent(
                EventSelectorUtil.selector(SdxDisableEncryptionProfileHandlerEvent.class),
                context.getSdxId(), context.getUserId());
    }

    @Override
    public String toString() {
        return "SdxDisableEncryptionProfileHandlerEvent{} " + super.toString();
    }
}
