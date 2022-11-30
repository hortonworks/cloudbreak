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

    private final String previousProxyConfigCrn;

    public ModifyProxyFlowChainTriggerEvent(String selector, Long stackId, String operationId, String previousProxyConfigCrn) {
        super(selector, stackId);
        this.operationId = operationId;
        this.previousProxyConfigCrn = previousProxyConfigCrn;
    }

    @JsonCreator
    public ModifyProxyFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("previousProxyConfigCrn") String previousProxyConfigCrn,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.operationId = operationId;
        this.previousProxyConfigCrn = previousProxyConfigCrn;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getPreviousProxyConfigCrn() {
        return previousProxyConfigCrn;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ModifyProxyFlowChainTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId) && Objects.equals(previousProxyConfigCrn, event.previousProxyConfigCrn));
    }

    @Override
    public String toString() {
        return "ModifyProxyFlowChainTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                "previousProxyConfigCrn='" + previousProxyConfigCrn + '\'' +
                "} " + super.toString();
    }
}
