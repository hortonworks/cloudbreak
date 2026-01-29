package com.sequenceiq.datalake.flow;

import java.util.List;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

public interface RetryableDatalakeFlowConfiguration<E extends Enum & FlowEvent> extends RetryableFlowConfiguration<E> {

    default List<E> getStackRetryEvents() {
        return List.of();
    }

    Class<E> getEventType();
}
