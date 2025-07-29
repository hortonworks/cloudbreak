package com.sequenceiq.freeipa.flow.freeipa.trust.cancel;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.AbstractCancelTrustSetupAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.CancelTrustSetupConfigurationAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.CancelTrustSetupFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.CancelTrustSetupFinishedAction;

public enum FreeIpaCancelTrustSetupState implements FlowState {
    INIT_STATE,
    CANCEL_TRUST_SETUP_CONFIGURATION_STATE(CancelTrustSetupConfigurationAction.class),
    CANCEL_TRUST_SETUP_FINISHED_STATE(CancelTrustSetupFinishedAction.class),
    CANCEL_TRUST_SETUP_FAILED_STATE(CancelTrustSetupFailedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractCancelTrustSetupAction<?>> action;

    FreeIpaCancelTrustSetupState(Class<? extends AbstractCancelTrustSetupAction<?>> action) {
        this.action = action;
    }

    FreeIpaCancelTrustSetupState() {
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
