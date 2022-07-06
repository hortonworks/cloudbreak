package com.sequenceiq.freeipa.flow.freeipa.backup.full.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class TriggerFullBackupEvent extends StackEvent {

    private final String operationId;

    private final boolean chained;

    private final boolean finalChain;

    @JsonCreator
    public TriggerFullBackupEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalChain") boolean finalChain) {
        super(selector, stackId);
        this.operationId = operationId;
        this.chained = chained;
        this.finalChain = finalChain;
    }

    public String getOperationId() {
        return operationId;
    }

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalChain() {
        return finalChain;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(TriggerFullBackupEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && chained == event.chained
                        && finalChain == event.finalChain);
    }

    @Override
    public String toString() {
        return "TriggerFullBackupEvent{" +
                "operationId='" + operationId + '\'' +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                "} " + super.toString();
    }
}
