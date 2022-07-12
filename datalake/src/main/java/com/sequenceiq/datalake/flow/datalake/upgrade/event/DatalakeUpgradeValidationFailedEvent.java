package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_VALIDATION_FAILED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeValidationFailedEvent extends SdxEvent {

    @JsonCreator
    public DatalakeUpgradeValidationFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return DATALAKE_UPGRADE_VALIDATION_FAILED_EVENT.event();
    }

    @Override
    public String toString() {
        return "DatalakeUpgradeValidationFailedEvent{} " + super.toString();
    }
}
