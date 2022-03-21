package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class UpgradeCcmTriggerEvent extends StackEvent {

    private final String operationId;

    private boolean chained;

    private boolean finalFlow = true;

    public UpgradeCcmTriggerEvent(String selector, String operationId, Long stackId) {
        super(selector, stackId);
        this.operationId = operationId;
    }

    public UpgradeCcmTriggerEvent(String selector, String operationId, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(UpgradeCcmTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    public UpgradeCcmTriggerEvent withIsChained(boolean chained) {
        this.chained = chained;
        return this;
    }

    public UpgradeCcmTriggerEvent withIsFinal(boolean finalFlow) {
        this.finalFlow = finalFlow;
        return this;
    }

    public boolean isFinal() {
        return finalFlow;
    }

    public boolean isChained() {
        return chained;
    }

    @Override
    public String toString() {
        return "UpgradeCcmTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ",chained='" + chained + '\'' +
                ",finalFlow='" + finalFlow + '\'' +
                "} " + super.toString();
    }

}
