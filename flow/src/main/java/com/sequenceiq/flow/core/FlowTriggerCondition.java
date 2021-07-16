package com.sequenceiq.flow.core;

public interface FlowTriggerCondition {
    FlowTriggerConditionResult isFlowTriggerable(Long stackId);
}
