package com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config;

import com.sequenceiq.cloudbreak.reactor.api.event.stack.DatalakeReachabilityValidationResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeReachabilityValidationEvent implements FlowEvent {
    DATALAKE_REACHABILITY_VALIDATION_EVENT(),
    DATALAKE_REACHABILITY_VALIDATION_FINISHED_EVENT(EventSelectorUtil.selector(DatalakeReachabilityValidationResult.class)),
    DATALAKE_REACHABILITY_VALIDATION_FAILED_EVENT(EventSelectorUtil.failureSelector(DatalakeReachabilityValidationResult.class)),
    DATALAKE_REACHABILITY_VALIDATION_FAILURE_HANDLED_EVENT();

    private final String event;

    DatalakeReachabilityValidationEvent() {
        this.event = name();
    }

    DatalakeReachabilityValidationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
