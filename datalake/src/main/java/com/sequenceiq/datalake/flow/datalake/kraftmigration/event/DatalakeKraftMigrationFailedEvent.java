package com.sequenceiq.datalake.flow.datalake.kraftmigration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeKraftMigrationFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public DatalakeKraftMigrationFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static DatalakeKraftMigrationFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeKraftMigrationFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "DatalakeKraftMigrationFailedEvent{} " + super.toString();
    }
}
