package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class SaltUpdateTriggerEvent extends StackEvent {

    private String operationId;

    private final boolean chained;

    private final boolean finalChain;

    public SaltUpdateTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
        chained = false;
        finalChain = false;
    }

    @JsonCreator
    public SaltUpdateTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalChain") boolean finalChain) {
        super(selector, stackId, accepted);
        this.chained = chained;
        this.finalChain = finalChain;
    }

    public SaltUpdateTriggerEvent withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
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
