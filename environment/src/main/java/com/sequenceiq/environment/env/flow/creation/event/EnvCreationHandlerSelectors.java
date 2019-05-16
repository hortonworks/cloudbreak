package com.sequenceiq.environment.env.flow.creation.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvCreationHandlerSelectors implements FlowEvent {
    CREATE_NETWORK_EVENT,
    CREATE_RDBMS_EVENT,
    CREATE_FREEIPA_EVENT;

    @Override
    public String event() {
        return name();
    }
}
