package com.sequenceiq.freeipa.flow.freeipa.trust.repair.event;

import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.TRUST_REPAIR_TRIGGER_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaTrustRepairEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public FreeIpaTrustRepairEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("operationId") String operationId) {
        super(TRUST_REPAIR_TRIGGER_EVENT, stackId);
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
