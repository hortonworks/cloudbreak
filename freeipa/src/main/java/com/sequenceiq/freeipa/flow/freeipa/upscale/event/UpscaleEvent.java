package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpscaleEvent extends StackEvent {
    private final Integer instanceCountByGroup;

    private final Boolean repair;

    private final String operationId;

    private final boolean chained;

    private final boolean finalChain;

    public UpscaleEvent(String selector, Long stackId, Integer instanceCountByGroup, Boolean repair, boolean chained, boolean finalChain, String operationId) {
        super(selector, stackId);
        this.instanceCountByGroup = instanceCountByGroup;
        this.repair = repair;
        this.chained = chained;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    public Integer getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public Boolean isRepair() {
        return repair;
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
        return "UpscaleEvent{" +
                "instanceCountByGroup=" + instanceCountByGroup +
                ", repair=" + repair +
                ", operationId='" + operationId + '\'' +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                "} " + super.toString();
    }
}
