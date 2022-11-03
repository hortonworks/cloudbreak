package com.sequenceiq.freeipa.flow.stack.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ModifyProxyFlowChainTriggerEvent extends StackEvent {

    private final String operationId;

    public ModifyProxyFlowChainTriggerEvent(String selector, Long stackId, String operationId) {
        super(selector, stackId);
        this.operationId = operationId;
    }

    @JsonCreator
    public ModifyProxyFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ModifyProxyFlowChainTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    @Override
    public String toString() {
        return "ModifyProxyFlowChainTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
