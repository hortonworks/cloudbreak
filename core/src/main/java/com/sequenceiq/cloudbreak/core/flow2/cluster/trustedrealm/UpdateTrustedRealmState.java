package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum UpdateTrustedRealmState implements FlowState {
    INIT_STATE,
    UPDATE_TRUSTED_REALM_STATE,
    UPDATE_TRUSTED_REALM_FINISHED_STATE,
    UPDATE_TRUSTED_REALM_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}

