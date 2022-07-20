package com.sequenceiq.flow.component.sleep;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class SleepTriggerCondition implements FlowTriggerCondition {

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        return FlowTriggerConditionResult.ok();
    }
}
