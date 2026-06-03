package com.sequenceiq.environment.environment.flow.modify.network.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvNetworkCidrsModificationHandlerSelectors implements FlowEvent {
    MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT,
    MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT;

    @Override
    public String event() {
        return name();
    }
}
