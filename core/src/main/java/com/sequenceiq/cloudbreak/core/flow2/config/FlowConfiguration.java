package com.sequenceiq.cloudbreak.core.flow2.config;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public interface FlowConfiguration<S extends FlowState, E extends FlowEvent> {
    Flow<S, E> createFlow(String flowId);
    E[] getEvents();
}
