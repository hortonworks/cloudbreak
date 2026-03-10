package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams;

import com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.handler.ResetJvmParamsResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ResetJvmParamsFlowEvent implements FlowEvent {

    RESET_JVM_PARAMS_EVENT,
    RESET_JVM_PARAMS_FINISHED_EVENT(EventSelectorUtil.selector(ResetJvmParamsResult.class)),
    RESET_JVM_PARAMS_FINALIZED_EVENT,
    RESET_JVM_PARAMS_FAIL_HANDLED_EVENT,
    RESET_JVM_PARAMS_FAILED_EVENT(EventSelectorUtil.selector(ResetJvmParamsFailedEvent.class));

    private final String event;

    ResetJvmParamsFlowEvent() {
        event = name();
    }

    ResetJvmParamsFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
