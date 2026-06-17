package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareUpgradeTriggerEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public PrepareUpgradeTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId) {
        super(selector, stackId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "PrepareUpgradeTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
