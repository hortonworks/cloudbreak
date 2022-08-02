package com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeUpgradePreparationFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public DatalakeUpgradePreparationFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static DatalakeUpgradePreparationFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeUpgradePreparationFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "DatalakeUpgradePreparationFailedEvent{} " + super.toString();
    }
}
