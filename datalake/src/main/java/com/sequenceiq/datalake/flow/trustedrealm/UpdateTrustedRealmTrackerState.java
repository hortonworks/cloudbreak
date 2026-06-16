package com.sequenceiq.datalake.flow.trustedrealm;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.datalake.flow.trustedrealm.action.UpdateTrustedRealmAction;
import com.sequenceiq.datalake.flow.trustedrealm.action.UpdateTrustedRealmFailureAction;
import com.sequenceiq.datalake.flow.trustedrealm.action.UpdateTrustedRealmSuccessAction;
import com.sequenceiq.datalake.flow.trustedrealm.action.UpdateTrustedRealmWaitAction;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum UpdateTrustedRealmTrackerState implements FlowState {

    INIT_STATE,
    UPDATE_TRUSTED_REALM_WAITING_STATE(UpdateTrustedRealmWaitAction.class),
    UPDATE_TRUSTED_REALM_SUCCESS_STATE(UpdateTrustedRealmSuccessAction.class),
    UPDATE_TRUSTED_REALM_FAILED_STATE(UpdateTrustedRealmFailureAction.class),
    FINAL_STATE;

    private Class<? extends UpdateTrustedRealmAction<?>> action;

    UpdateTrustedRealmTrackerState() {
    }

    UpdateTrustedRealmTrackerState(Class<? extends UpdateTrustedRealmAction<?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
