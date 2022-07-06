package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
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

    @JsonCreator
    public UpgradeCcmTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
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

    public boolean isFinalFlow() {
        return finalFlow;
    }

    public boolean isChained() {
        return chained;
    }

    public void setChained(boolean chained) {
        this.chained = chained;
    }

    public void setFinalFlow(boolean finalFlow) {
        this.finalFlow = finalFlow;
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
