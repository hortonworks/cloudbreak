package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class UpgradeCcmFlowChainTriggerEvent extends StackEvent {

    private final String operationId;

    public UpgradeCcmFlowChainTriggerEvent(String selector, String operationId, Long stackId) {
        super(selector, stackId);
        this.operationId = operationId;
    }

    public UpgradeCcmFlowChainTriggerEvent(String selector, String operationId, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(UpgradeCcmFlowChainTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    @Override
    public String toString() {
        return "UpgradeCcmFlowChainTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }

}
