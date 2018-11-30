package com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

enum ChangePrimaryGatewayState implements FlowState {
    INIT_STATE,
    CHANGE_PRIMARY_GATEWAY_STATE,
    WAITING_FOR_AMBARI_SERVER_STATE,
    CHANGE_PRIMARY_GATEWAY_FINISHED_STATE,
    CHANGE_PRIMARY_GATEWAY_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
