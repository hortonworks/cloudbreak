package com.sequenceiq.freeipa.flow.freeipa.trust.finish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FinishTrustSetupEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public FinishTrustSetupEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("operationId") String operationId) {
        super(stackId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "FinishCrossRealmTrustEvent{" +
                "operationId='" + operationId + '\'' +
                '}';
    }
}
