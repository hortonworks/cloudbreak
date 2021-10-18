package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

import reactor.rx.Promise;

public class StackDownscaleTriggerEvent extends StackScaleTriggerEvent {

    private final Set<Long> privateIds;

    public StackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId, hostGroup, adjustment, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, adjustment.longValue()));
        privateIds = null;
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Set<Long> privateIds) {
        super(selector, stackId, instanceGroup, null, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) privateIds.size()));
        this.privateIds = privateIds;
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Set<Long> privateIds, Promise<AcceptResult> accepted) {
        super(selector, stackId, instanceGroup, null, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) privateIds.size()), accepted);
        this.privateIds = privateIds;
    }

    @Override
    public StackDownscaleTriggerEvent setRepair() {
        super.setRepair();
        return this;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }
}
