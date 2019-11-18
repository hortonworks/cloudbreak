package com.sequenceiq.environment.environment.flow.creation.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvCreationStateSelectors implements FlowEvent {
    START_ENVIRONMENT_VALIDATION_EVENT,
    START_NETWORK_CREATION_EVENT,
    START_PUBLICKEY_CREATION_EVENT,
    START_FREEIPA_CREATION_EVENT,
    FINISH_ENV_CREATION_EVENT,
    FINALIZE_ENV_CREATION_EVENT,
    FAILED_ENV_CREATION_EVENT,
    HANDLED_FAILED_ENV_CREATION_EVENT;

    @Override
    public String event() {
        return name();
    }
}
