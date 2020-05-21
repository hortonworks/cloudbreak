package com.sequenceiq.environment.environment.flow.creation.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvCreationHandlerSelectors implements FlowEvent {
    INITIALIZE_ENVIRONMENT_EVENT,
    VALIDATE_ENVIRONMENT_EVENT,
    CREATE_PREREQUISITES_EVENT,
    CREATE_NETWORK_EVENT,
    CREATE_PUBLICKEY_EVENT,
    CREATE_FREEIPA_EVENT;

    @Override
    public String event() {
        return name();
    }
}
