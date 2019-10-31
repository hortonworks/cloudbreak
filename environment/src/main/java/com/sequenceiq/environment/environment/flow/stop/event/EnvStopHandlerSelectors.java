package com.sequenceiq.environment.environment.flow.stop.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvStopHandlerSelectors implements FlowEvent {

    STOP_DATAHUB_HANDLER_EVENT,
    STOP_DATALAKE_HANDLER_EVENT,
    STOP_FREEIPA_HANDLER_EVENT;

    @Override
    public String event() {
        return name();
    }
}
