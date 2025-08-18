package com.sequenceiq.freeipa.flow.freeipa.trust.repair.event;

import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.REPAIR_TRUST_TRIGGER_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class TrustRepairEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public TrustRepairEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("operationId") String operationId) {
        super(REPAIR_TRUST_TRIGGER_EVENT, stackId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "TrustRepairEvent{" +
                "operationId='" + operationId + '\'' +
                '}';
    }
}
