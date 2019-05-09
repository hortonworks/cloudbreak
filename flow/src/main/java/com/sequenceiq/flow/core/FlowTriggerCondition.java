package com.sequenceiq.flow.core;

public interface FlowTriggerCondition {
    boolean isFlowTriggerable(Long stackId);
}
