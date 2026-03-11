package com.sequenceiq.datalake.flow.datalake.kraftmigration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeKraftMigrationWaitRequest extends SdxEvent {

    @JsonCreator
    public DatalakeKraftMigrationWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static DatalakeKraftMigrationWaitRequest from(SdxContext context) {
        return new DatalakeKraftMigrationWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String toString() {
        return "DatalakeKraftMigrationWaitRequest{} " + super.toString();
    }
}
