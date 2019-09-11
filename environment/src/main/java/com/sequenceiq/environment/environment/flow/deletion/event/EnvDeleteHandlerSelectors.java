package com.sequenceiq.environment.environment.flow.deletion.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvDeleteHandlerSelectors implements FlowEvent {

    DELETE_NETWORK_EVENT,
    DELETE_RDBMS_EVENT,
    DELETE_FREEIPA_EVENT,
    DELETE_IDBROKER_MAPPINGS_EVENT;

    @Override
    public String event() {
        return name();
    }

}
