package com.sequenceiq.environment.environment.flow.start.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvStartStateSelectors implements FlowEvent {
    ENV_START_DATAHUB_EVENT,
    ENV_START_DATALAKE_EVENT,
    ENV_START_FREEIPA_EVENT,
    FINISH_ENV_START_EVENT,
    FINALIZE_ENV_START_EVENT,
    HANDLED_FAILED_ENV_START_EVENT,
    FAILED_ENV_START_EVENT;

    @Override
    public String event() {
        return name();
    }
}
