package com.sequenceiq.datalake.flow.upgrade.database.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeDatabaseServerWaitRequest extends SdxEvent {

    @JsonCreator
    public UpgradeDatabaseServerWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static UpgradeDatabaseServerWaitRequest from(SdxContext context) {
        return new UpgradeDatabaseServerWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerWaitRequest{} " + super.toString();
    }

}