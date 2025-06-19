package com.sequenceiq.freeipa.flow.freeipa.trust.finish;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.action.AbstractFinishTrustSetupAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.action.FinishTrustSetupAddTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.action.FinishTrustSetupFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.action.FinishTrustSetupFinishedAction;

public enum FreeIpaFinishTrustSetupState implements FlowState {
    INIT_STATE,
    ADD_TRUST_STATE(FinishTrustSetupAddTrustAction.class),
    FINISH_TRUST_SETUP_FINISHED_STATE(FinishTrustSetupFinishedAction.class),
    FINISH_TRUST_SETUP_FAILED_STATE(FinishTrustSetupFailedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractFinishTrustSetupAction<?>> action;

    FreeIpaFinishTrustSetupState(Class<? extends AbstractFinishTrustSetupAction<?>> action) {
        this.action = action;
    }

    FreeIpaFinishTrustSetupState() {
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
