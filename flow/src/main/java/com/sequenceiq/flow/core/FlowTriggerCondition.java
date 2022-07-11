package com.sequenceiq.flow.core;

import com.sequenceiq.cloudbreak.common.event.Payload;

public interface FlowTriggerCondition {
    FlowTriggerConditionResult isFlowTriggerable(Payload payload);

}
