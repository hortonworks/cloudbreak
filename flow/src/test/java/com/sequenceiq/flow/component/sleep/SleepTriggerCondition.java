package com.sequenceiq.flow.component.sleep;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class SleepTriggerCondition implements FlowTriggerCondition {

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Long stackId) {
        return FlowTriggerConditionResult.OK;
    }
}
