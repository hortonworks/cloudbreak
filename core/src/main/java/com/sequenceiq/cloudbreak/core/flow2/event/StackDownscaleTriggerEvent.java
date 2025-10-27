package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

public class StackDownscaleTriggerEvent extends StackScaleTriggerEvent {

    private final boolean purgeZombies;

    public StackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment, String triggeredStackVariant) {
        super(selector, stackId, hostGroupWithAdjustment, Collections.emptyMap(), Collections.emptyMap(),
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null), triggeredStackVariant);
        this.purgeZombies = false;
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
            Map<String, Set<Long>> hostGroupWithPrivateIds, Map<String, Set<String>> hostGroupWithHostNames,
            String triggeredStackVariant, boolean purgeZombies) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames,
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null), triggeredStackVariant);
        this.purgeZombies = purgeZombies;
    }

    @JsonCreator
    public StackDownscaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroupsWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("hostGroupsWithPrivateIds") Map<String, Set<Long>> hostGroupWithPrivateIds,
            @JsonProperty("hostGroupsWithHostNames") Map<String, Set<String>> hostGroupWithHostNames,
            @JsonProperty("triggeredStackVariant") String triggeredStackVariant,
            @JsonProperty("purgeZombies") boolean purgeZombies,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames,
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null), triggeredStackVariant, accepted);
        this.purgeZombies = purgeZombies;
    }

    @Override
    public StackDownscaleTriggerEvent setRepair() {
        super.setRepair();
        return this;
    }

    @Override
    public Map<String, Set<Long>> getHostGroupsWithPrivateIds() {
        if (super.getHostGroupsWithPrivateIds() == null) {
            super.setHostGroupsWithPrivateIds(Map.of());
        }
        return super.getHostGroupsWithPrivateIds();
    }

    public boolean isPurgeZombies() {
        return purgeZombies;
    }
}
