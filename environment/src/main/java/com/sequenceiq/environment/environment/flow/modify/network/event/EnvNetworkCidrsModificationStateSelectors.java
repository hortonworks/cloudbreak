package com.sequenceiq.environment.environment.flow.modify.network.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvNetworkCidrsModificationStateSelectors implements FlowEvent {
    START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT,
    START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT,
    START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT,
    FINISH_MODIFY_NETWORK_CIDRS_EVENT,
    FINALIZE_MODIFY_NETWORK_CIDRS_EVENT,
    FAILED_MODIFY_NETWORK_CIDRS_EVENT,
    HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT;

    @Override
    public String event() {
        return name();
    }
}
