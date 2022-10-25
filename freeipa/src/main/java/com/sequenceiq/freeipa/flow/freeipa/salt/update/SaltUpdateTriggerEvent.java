package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class SaltUpdateTriggerEvent extends StackEvent {

    private final String operationId;

    private final boolean chained;

    private final boolean finalChain;

    public SaltUpdateTriggerEvent(Long stackId) {
        super(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), stackId);
        chained = false;
        finalChain = false;
        operationId = null;
    }

    @JsonCreator
    public SaltUpdateTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalChain") boolean finalChain,
            @JsonProperty("operationId") String operationId) {
        super(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), stackId, accepted);
        this.chained = chained;
        this.finalChain = finalChain;
        this.operationId = operationId;
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
        return isClassAndEqualsEvent(SaltUpdateTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && chained == event.chained
                        && finalChain == event.finalChain);
    }

    @Override
    public String toString() {
        return "SaltUpdateTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                "} " + super.toString();
    }
}
