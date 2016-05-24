package com.sequenceiq.cloudbreak.core.flow2;

public interface FlowTriggerCondition {
    boolean isFlowTriggerable(Long stackId);
}
