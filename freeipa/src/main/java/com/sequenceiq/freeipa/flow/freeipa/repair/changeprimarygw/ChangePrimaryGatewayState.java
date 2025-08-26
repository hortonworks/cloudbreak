package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum ChangePrimaryGatewayState implements FlowState {
    INIT_STATE,
    CHANGE_PRIMARY_GATEWAY_STATE_STARTING,
    CHANGE_PRIMARY_GATEWAY_SELECTION,
    CHANGE_PRIMARY_GATEWAY_METADATA_STATE,
    CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_STATE,
    CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_STATE,
    CHANGE_PRIMARY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_STATE,
    CHANGE_PRIMARY_GATEWAY_FINISHED_STATE,
    CHANGE_PRIMARY_GATEWAY_FAIL_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
