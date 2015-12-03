package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.List;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.Flow;

public interface FlowConfiguration<S extends FlowState, E extends FlowEvent> {
    Flow<S, E> createFlow(String flowId);
    List<E> getFlowTriggerEvents();
    E[] getEvents();
}
