package com.sequenceiq.environment.environment.flow.stop.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvStopStateSelectors implements FlowEvent {
    ENV_STOP_DATAHUB_EVENT,
    ENV_STOP_DATALAKE_EVENT,
    ENV_STOP_FREEIPA_EVENT,
    FINISH_ENV_STOP_EVENT,
    FINALIZE_ENV_STOP_EVENT,
    HANDLED_FAILED_ENV_STOP_EVENT,
    FAILED_ENV_STOP_EVENT;

    @Override
    public String event() {
        return name();
    }
}
