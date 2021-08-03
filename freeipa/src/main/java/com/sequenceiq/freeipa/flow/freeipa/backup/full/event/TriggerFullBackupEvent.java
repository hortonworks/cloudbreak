package com.sequenceiq.freeipa.flow.freeipa.backup.full.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class TriggerFullBackupEvent extends StackEvent {
    private final String operationId;

    private final boolean chained;

    private final boolean finalChain;

    public TriggerFullBackupEvent(String selector, Long stackId, String operationId, boolean chained, boolean finalChain) {
        super(selector, stackId);
        this.operationId = operationId;
        this.chained = chained;
        this.finalChain = finalChain;
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
        return "TriggerFullBackupEvent{" +
                "operationId='" + operationId + '\'' +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                "} " + super.toString();
    }
}
