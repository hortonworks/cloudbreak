package com.sequenceiq.datalake.flow.datalake.kraftmigration.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.kraftmigration.KraftMigrationOperationType;

public class DatalakeKraftMigrationStartEvent extends SdxEvent {

    private final KraftMigrationOperationType operationType;

    @JsonCreator
    public DatalakeKraftMigrationStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationType") KraftMigrationOperationType operationType) {
        super(selector, sdxId, userId);
        this.operationType = operationType;
    }

    public KraftMigrationOperationType getOperationType() {
        return operationType;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeKraftMigrationStartEvent.class, other,
                event -> Objects.equals(operationType, event.operationType));
    }

    @Override
    public String toString() {
        return "DatalakeKraftMigrationStartEvent{" +
                "operationType=" + operationType +
                "} " + super.toString();
    }
}
