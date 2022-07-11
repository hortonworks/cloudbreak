package com.sequenceiq.flow.core;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;

@Component
public class DefaultFlowTriggerCondition implements FlowTriggerCondition {
    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        return FlowTriggerConditionResult.ok();
    }
}
