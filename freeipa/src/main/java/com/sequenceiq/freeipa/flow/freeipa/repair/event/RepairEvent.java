package com.sequenceiq.freeipa.flow.freeipa.repair.event;

import java.util.List;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RepairEvent extends StackEvent {
    private final String operationId;

    private final int instanceCountByGroup;

    private final List<String> repairInstanceIds;

    private final List<String> additionalTerminatedInstanceIds;

    public RepairEvent(String selector, Long stackId, String operationId, int instanceCountByGroup, List<String> repairInstanceIds,
            List<String> additionalTerminatedInstanceIds) {
        super(selector, stackId);
        this.operationId = operationId;
        this.instanceCountByGroup = instanceCountByGroup;
        this.repairInstanceIds = repairInstanceIds;
        this.additionalTerminatedInstanceIds = additionalTerminatedInstanceIds;
    }

    public String getOperationId() {
        return operationId;
    }

    public int getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public List<String> getRepairInstanceIds() {
        return repairInstanceIds;
    }

    public List<String> getAdditionalTerminatedInstanceIds() {
        return additionalTerminatedInstanceIds;
    }
}
