package com.sequenceiq.environment.environment.flow.creation.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvCreationHandlerSelectors implements FlowEvent {
    CREATE_NETWORK_EVENT,
    CREATE_FREEIPA_EVENT;

    @Override
    public String event() {
        return name();
    }
}
