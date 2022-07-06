package com.sequenceiq.freeipa.flow.stack.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UserDataUpdateRequest extends StackEvent {
    private String operationId;

    private final Tunnel oldTunnel;

    private boolean chained;

    private boolean finalFlow = true;

    public UserDataUpdateRequest(Long stackId, Tunnel oldTunnel) {
        super(stackId);
        this.operationId = null;
        this.oldTunnel = oldTunnel;
    }

    @JsonCreator
    public UserDataUpdateRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel) {
        super(selector, stackId);
        this.operationId = null;
        this.oldTunnel = oldTunnel;
    }

    public UserDataUpdateRequest withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public UserDataUpdateRequest withIsChained(boolean chained) {
        this.chained = chained;
        return this;
    }

    public UserDataUpdateRequest withIsFinal(boolean finalFlow) {
        this.finalFlow = finalFlow;
        return this;
    }

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalFlow() {
        return finalFlow;
    }

    public void setChained(boolean chained) {
        this.chained = chained;
    }

    public void setFinalFlow(boolean finalFlow) {
        this.finalFlow = finalFlow;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public String toString() {
        return "UserDataUpdateRequest{" +
                "operationId='" + operationId + '\'' +
                ", oldTunnel=" + oldTunnel +
                ", chained=" + chained +
                ", finalFlow=" + finalFlow +
                "} " + super.toString();
    }
}
