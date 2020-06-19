package com.sequenceiq.freeipa.flow.freeipa.repair.event;

import java.util.List;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RepairEvent extends StackEvent {
    private final String operationId;

    private final int instanceCountByGroup;

    private final List<String> instanceIds;

    private final List<String> additionalTermiantedInstanceIds;

    public RepairEvent(String selector, Long stackId, String operationId, int instanceCountByGroup, List<String> instanceIds,
            List<String> additionalTerminatedInstanceIds) {
        super(selector, stackId);
        this.operationId = operationId;
        this.instanceCountByGroup = instanceCountByGroup;
        this.instanceIds = instanceIds;
        this.additionalTermiantedInstanceIds = additionalTerminatedInstanceIds;
    }

    public String getOperationId() {
        return operationId;
    }

    public int getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public List<String> getAdditionalTermiantedInstanceIds() {
        return additionalTermiantedInstanceIds;
    }
}
