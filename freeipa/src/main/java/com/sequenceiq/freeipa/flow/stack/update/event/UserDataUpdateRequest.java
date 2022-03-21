package com.sequenceiq.freeipa.flow.stack.update.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UserDataUpdateRequest extends StackEvent {
    private String operationId;

    private boolean chained;

    private boolean finalFlow = true;

    public UserDataUpdateRequest(Long stackId) {
        super(stackId);
    }

    public UserDataUpdateRequest(String selector, Long stackId) {
        super(selector, stackId);
    }

    public UserDataUpdateRequest withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getOperationId() {
        return operationId;
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

    public boolean isFinal() {
        return finalFlow;
    }

    @Override
    public String toString() {
        return "UserDataUpdateRequest{" +
                "operationId='" + operationId + '\'' +
                ",chained='" + chained + '\'' +
                ",finalFlow='" + finalFlow + '\'' +
                "} " + super.toString();
    }
}
