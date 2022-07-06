package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmSuccessEvent extends SdxEvent {

    @JsonCreator
    public UpgradeCcmSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return UpgradeCcmSuccessEvent.class.getSimpleName();
    }
}
