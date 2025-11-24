package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskValidationTriggerEvent extends StackEvent {

    private final Map<String, Set<String>> repairableGroupsWithHostNames;

    public DiskValidationTriggerEvent(
            String selector,
            Long stackId,
            Map<String, Set<String>> repairableGroupsWithHostNames,
            Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.repairableGroupsWithHostNames = repairableGroupsWithHostNames;
    }

    @JsonCreator
    public DiskValidationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("repairableGroupsWithHostNames") Map<String, Set<String>> repairableGroupsWithHostNames) {
        super(selector, stackId);
        this.repairableGroupsWithHostNames = repairableGroupsWithHostNames;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(DiskValidationTriggerEvent.class, other);
    }

    public Map<String, Set<String>> getRepairableGroupsWithHostNames() {
        return repairableGroupsWithHostNames;
    }
}
