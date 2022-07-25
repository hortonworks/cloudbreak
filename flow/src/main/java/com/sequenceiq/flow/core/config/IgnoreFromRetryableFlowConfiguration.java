package com.sequenceiq.flow.core.config;

import java.util.Set;

import com.sequenceiq.flow.core.FlowEvent;

public interface IgnoreFromRetryableFlowConfiguration<E extends FlowEvent> {
    Set<E> getIgnoredEvents();
}
