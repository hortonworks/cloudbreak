package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class SaltUpdateTriggerEvent extends StackEvent {

    private String operationId;

    private final boolean chained;

    private final boolean finalChain;

    public SaltUpdateTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
        chained = false;
        finalChain = false;
    }

    public SaltUpdateTriggerEvent(String selector, Long stackId, Promise<AcceptResult> accepted, boolean chained, boolean finalChain) {
        super(selector, stackId, accepted);
        this.chained = chained;
        this.finalChain = finalChain;
    }

    public SaltUpdateTriggerEvent withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalChain() {
        return finalChain;
    }

    @Override
    public String toString() {
        return "SaltUpdateTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                "} " + super.toString();
    }
}
