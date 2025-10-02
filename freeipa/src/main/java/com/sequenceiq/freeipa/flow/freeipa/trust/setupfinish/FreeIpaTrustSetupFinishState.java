package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action.FreeIpaTrustSetupFinishAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action.FreeIpaTrustSetupFinishBaseAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action.FreeIpaTrustSetupFinishFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action.FreeIpaTrustSetupFinishSuccessAction;

public enum FreeIpaTrustSetupFinishState implements FlowState {
    INIT_STATE,
    TRUST_SETUP_FINISH_ADD_TRUST_STATE(FreeIpaTrustSetupFinishAction.class),
    TRUST_SETUP_FINISH_FINISHED_STATE(FreeIpaTrustSetupFinishSuccessAction.class),
    TRUST_SETUP_FINISH_FAILED_STATE(FreeIpaTrustSetupFinishFailedAction.class),
    FINAL_STATE;

    private Class<? extends FreeIpaTrustSetupFinishBaseAction<?>> action;

    FreeIpaTrustSetupFinishState(Class<? extends FreeIpaTrustSetupFinishBaseAction<?>> action) {
        this.action = action;
    }

    FreeIpaTrustSetupFinishState() {
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
