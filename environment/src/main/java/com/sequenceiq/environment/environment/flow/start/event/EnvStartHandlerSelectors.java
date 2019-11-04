package com.sequenceiq.environment.environment.flow.start.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvStartHandlerSelectors implements FlowEvent {

    START_DATAHUB_HANDLER_EVENT,
    START_DATALAKE_HANDLER_EVENT,
    START_FREEIPA_HANDLER_EVENT;

    @Override
    public String event() {
        return name();
    }
}
