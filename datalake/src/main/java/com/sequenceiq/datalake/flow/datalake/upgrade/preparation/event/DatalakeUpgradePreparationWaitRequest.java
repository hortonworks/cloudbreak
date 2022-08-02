package com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradePreparationWaitRequest extends SdxEvent {

    @JsonCreator
    public DatalakeUpgradePreparationWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static DatalakeUpgradePreparationWaitRequest from(SdxContext context) {
        return new DatalakeUpgradePreparationWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String toString() {
        return "DatalakeUpgradePreparationWaitRequest{" +
                "} " + super.toString();
    }
}
