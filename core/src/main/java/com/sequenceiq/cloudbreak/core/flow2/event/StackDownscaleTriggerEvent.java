package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

import reactor.rx.Promise;

public class StackDownscaleTriggerEvent extends StackScaleTriggerEvent {

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private Set<Long> privateIds;

    public StackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment, String triggeredStackVariant) {
        super(selector, stackId, hostGroupWithAdjustment, Collections.emptyMap(), Collections.emptyMap(),
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null), triggeredStackVariant);
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
            Map<String, Set<Long>> hostGroupWithPrivateIds, Map<String, Set<String>> hostGroupWithHostNames, String triggeredStackVariant) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames,
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null), triggeredStackVariant);
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
            Map<String, Set<Long>> hostGroupWithPrivateIds, Map<String, Set<String>> hostGroupWithHostNames, String triggeredStackVariant,
            Promise<AcceptResult> accepted) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames,
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null), triggeredStackVariant, accepted);
    }

    @Override
    public StackDownscaleTriggerEvent setRepair() {
        super.setRepair();
        return this;
    }

    @Override
    public Map<String, Set<Long>> getHostGroupsWithPrivateIds() {
        if (super.getHostGroupsWithPrivateIds() == null) {
            if (getInstanceGroup() != null && privateIds != null) {
                super.setHostGroupsWithPrivateIds(Map.of(getInstanceGroup(), privateIds));
            } else {
                super.setHostGroupsWithPrivateIds(Map.of());
            }
        }
        return super.getHostGroupsWithPrivateIds();
    }

}
