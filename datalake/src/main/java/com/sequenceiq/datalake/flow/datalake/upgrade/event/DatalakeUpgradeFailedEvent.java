package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_FAILED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeUpgradeFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public DatalakeUpgradeFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static DatalakeUpgradeFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeUpgradeFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return DATALAKE_UPGRADE_FAILED_EVENT.event();
    }

    @Override
    public String toString() {
        return "DatalakeUpgradeFailedEvent{} " + super.toString();
    }
}
