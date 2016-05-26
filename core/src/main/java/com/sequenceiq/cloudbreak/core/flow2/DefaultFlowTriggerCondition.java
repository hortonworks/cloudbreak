package com.sequenceiq.cloudbreak.core.flow2;

import org.springframework.stereotype.Component;

@Component
public class DefaultFlowTriggerCondition implements FlowTriggerCondition {
    @Override
    public boolean isFlowTriggerable(Long stackId) {
        return true;
    }
}
