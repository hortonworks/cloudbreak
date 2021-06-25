package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class SaltUpdateTriggerEvent extends StackEvent {

    private String operationId;

    public SaltUpdateTriggerEvent(Long stackId) {
        super(stackId);
    }

    public SaltUpdateTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    public SaltUpdateTriggerEvent(String selector, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }

    public SaltUpdateTriggerEvent withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "SaltUpdateTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
