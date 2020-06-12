package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import com.sequenceiq.flow.core.FlowState;

public enum ChangePrimaryGatewayState implements FlowState {
    INIT_STATE,
    CHANGE_PRIMARY_GATEWAY_STATE_STARTING,
    CHANGE_PRIMARY_GATEWAY_SELECTION,
    CHANGE_PRIMARY_GATEWAY_METADATA_STATE,
    CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_STATE,
    CHANGE_PRIMARY_GATEWAY_FINISHED_STATE,
    CHANGE_PRIMARY_GATEWAY_FAIL_STATE,
    FINAL_STATE;
}
