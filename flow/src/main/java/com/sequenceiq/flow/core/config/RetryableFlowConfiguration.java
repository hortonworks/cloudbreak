package com.sequenceiq.flow.core.config;

import com.sequenceiq.flow.core.FlowEvent;

public interface RetryableFlowConfiguration<E extends Enum & FlowEvent> {
    E getRetryableEvent();
}
