package com.sequenceiq.cloudbreak.core.flow2.config;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;

public interface FlowConfiguration<E extends FlowEvent> {
    Flow createFlow(String flowId);

    FlowTriggerCondition getFlowTriggerCondition();

    E[] getEvents();

    E[] getInitEvents();
}
