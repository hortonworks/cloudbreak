package com.sequenceiq.freeipa.flow.freeipa.migration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailureEvent;

public class MultiAzMigrationInitFailedEvent extends FreeIpaFailureEvent {

    @JsonCreator
    public MultiAzMigrationInitFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception exception) {
        super(stackId, failureType, exception);
    }

    public MultiAzMigrationInitFailedEvent(Long stackId, Exception exception) {
        super(stackId, FailureType.ERROR, exception);
    }
}
