package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class UpgradeCcmFlowChainTriggerEvent extends StackEvent {

    private final String operationId;

    private final Tunnel oldTunnel;

    public UpgradeCcmFlowChainTriggerEvent(String selector, String operationId, Long stackId, Tunnel oldTunnel) {
        super(selector, stackId);
        this.operationId = operationId;
        this.oldTunnel = oldTunnel;
    }

    public UpgradeCcmFlowChainTriggerEvent(String selector, String operationId, Long stackId, Tunnel oldTunnel, Promise<AcceptResult> accepted) {
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
        return isClassAndEqualsEvent(UpgradeCcmFlowChainTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    @Override
    public String toString() {
        return "UpgradeCcmFlowChainTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ", oldTunnel=" + oldTunnel +
                "} " + super.toString();
    }
}
