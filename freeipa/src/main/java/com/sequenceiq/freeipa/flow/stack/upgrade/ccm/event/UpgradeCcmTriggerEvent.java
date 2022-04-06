package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class UpgradeCcmTriggerEvent extends StackEvent {

    private final String operationId;

    private final Tunnel oldTunnel;

    private boolean chained;

    private boolean finalFlow = true;

    public UpgradeCcmTriggerEvent(String selector, String operationId, Long stackId, Tunnel oldTunnel) {
        super(selector, stackId);
        this.operationId = operationId;
        this.oldTunnel = oldTunnel;
    }

    public UpgradeCcmTriggerEvent(String selector, String operationId, Long stackId, Tunnel oldTunnel, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.operationId = operationId;
        this.oldTunnel = oldTunnel;
    }

    public String getOperationId() {
        return operationId;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
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
                ", oldTunnel=" + oldTunnel +
                ", chained=" + chained +
                ", finalFlow=" + finalFlow +
                "} " + super.toString();
    }
}
