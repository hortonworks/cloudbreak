package com.sequenceiq.environment.environment.flow.modify.network;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvNetworkCidrsModificationState implements FlowState {
    INIT_STATE,
    ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_START_STATE,
    NETWORK_CIDRS_MODIFICATION_FREEIPA_STATE,
    NETWORK_CIDRS_MODIFICATION_DATALAKE_AND_DATAHUBS_STATE,
    NETWORK_CIDRS_MODIFICATION_FINISHED_STATE,
    NETWORK_CIDRS_MODIFICATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
