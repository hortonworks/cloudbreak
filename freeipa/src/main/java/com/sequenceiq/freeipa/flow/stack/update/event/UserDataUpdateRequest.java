package com.sequenceiq.freeipa.flow.stack.update.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UserDataUpdateRequest extends StackEvent {
    private String operationId;

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

    @Override
    public String toString() {
        return "UserDataUpdateRequest{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
