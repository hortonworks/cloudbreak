package com.sequenceiq.datalake.flow.upgrade.database.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeDatabaseServerWaitSuccessEvent extends SdxEvent {

    @JsonCreator
    public SdxUpgradeDatabaseServerWaitSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String toString() {
        return "SdxUpgradeDatabaseServerSuccessEvent{} " + super.toString();
    }

}