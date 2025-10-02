package com.sequenceiq.freeipa.flow.freeipa.trust.cancel;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.FreeIpaTrustCancelAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.FreeIpaTrustCancelConfigurationAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.FreeIpaTrustCancelFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action.FreeIpaTrustCancelFinishedAction;

public enum FreeIpaTrustCancelState implements FlowState {
    INIT_STATE,
    TRUST_CANCEL_CONFIGURATION_STATE(FreeIpaTrustCancelConfigurationAction.class),
    TRUST_CANCEL_FINISHED_STATE(FreeIpaTrustCancelFinishedAction.class),
    TRUST_CANCEL_FAILED_STATE(FreeIpaTrustCancelFailedAction.class),
    FINAL_STATE;

    private Class<? extends FreeIpaTrustCancelAction<?>> action;

    FreeIpaTrustCancelState(Class<? extends FreeIpaTrustCancelAction<?>> action) {
        this.action = action;
    }

    FreeIpaTrustCancelState() {
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
