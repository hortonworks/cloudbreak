package com.sequenceiq.cloudbreak.core.flow2.config;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public interface FlowConfiguration<E extends FlowEvent> {
    Flow createFlow(String flowId);

    E[] getEvents();
}
