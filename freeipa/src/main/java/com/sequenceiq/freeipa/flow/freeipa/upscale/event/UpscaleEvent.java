package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpscaleEvent extends StackEvent {
    private final Integer instanceCountByGroup;

    private final Boolean repair;

    private final String operationId;

    public UpscaleEvent(String selector, Long stackId, Integer instanceCountByGroup, Boolean repair, String operationId) {
        super(selector, stackId);
        this.instanceCountByGroup = instanceCountByGroup;
        this.repair = repair;
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
}
