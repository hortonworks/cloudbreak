package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_SUCCESS_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeSuccessEvent extends SdxEvent {

    @JsonCreator
    public DatalakeUpgradeSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return DATALAKE_UPGRADE_SUCCESS_EVENT.event();
    }

    @Override
    public String toString() {
        return "DatalakeUpgradeSuccessEvent{} " + super.toString();
    }
}
