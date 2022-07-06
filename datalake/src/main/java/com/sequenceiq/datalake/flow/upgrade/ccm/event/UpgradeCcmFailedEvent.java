package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class UpgradeCcmFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public UpgradeCcmFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static UpgradeCcmFailedEvent from(SdxEvent event, Exception exception) {
        return new UpgradeCcmFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return UpgradeCcmFailedEvent.class.getSimpleName();
    }
}
