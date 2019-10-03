package com.sequenceiq.flow.core.config;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.RestartAction;

public interface FlowConfiguration<E extends FlowEvent> {
    Flow createFlow(String flowId, Long stackId);

    FlowTriggerCondition getFlowTriggerCondition();

    E[] getEvents();

    E[] getInitEvents();

    RestartAction getRestartAction(String event);

    String getDisplayName();
}
