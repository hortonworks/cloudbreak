package com.sequenceiq.cloudbreak.core.flow2.config;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public interface RetryableFlowConfiguration<E extends FlowEvent> {
    E getFailHandledEvent();
}
