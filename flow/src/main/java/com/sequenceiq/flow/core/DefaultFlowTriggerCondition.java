package com.sequenceiq.flow.core;

import org.springframework.stereotype.Component;

@Component
public class DefaultFlowTriggerCondition implements FlowTriggerCondition {
    @Override
    public boolean isFlowTriggerable(Long stackId) {
        return true;
    }
}
