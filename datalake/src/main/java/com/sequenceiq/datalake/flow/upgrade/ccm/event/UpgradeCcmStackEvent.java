package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmStackEvent extends SdxEvent {

    @JsonCreator
    public UpgradeCcmStackEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(selector, sdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(UpgradeCcmStackEvent.class, other);
    }
}
