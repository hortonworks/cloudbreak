package com.sequenceiq.freeipa.flow.freeipa.repair.event;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RepairEvent extends StackEvent {

    private final String operationId;

    private final int instanceCountByGroup;

    private final List<String> repairInstanceIds;

    private final List<String> additionalTerminatedInstanceIds;

    @JsonCreator
    public RepairEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("instanceCountByGroup") int instanceCountByGroup,
            @JsonProperty("repairInstanceIds") List<String> repairInstanceIds,
            @JsonProperty("additionalTerminatedInstanceIds") List<String> additionalTerminatedInstanceIds) {
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

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(RepairEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && instanceCountByGroup == event.instanceCountByGroup
                        && Objects.equals(repairInstanceIds, event.repairInstanceIds)
                        && Objects.equals(additionalTerminatedInstanceIds, event.additionalTerminatedInstanceIds));
    }

}
