package com.sequenceiq.freeipa.flow.freeipa.trust.setup;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.FreeIpaTrustSetupBaseAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.FreeIpaTrustSetupConfigureDnsAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.FreeIpaTrustSetupFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.FreeIpaTrustSetupFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.FreeIpaTrustSetupPrepareServerAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.FreeIpaTrustSetupValidationAction;

public enum FreeIpaTrustSetupState implements FlowState {
    INIT_STATE,
    TRUST_SETUP_VALIDATION_STATE(FreeIpaTrustSetupValidationAction.class),
    TRUST_SETUP_PREPARE_IPA_SERVER_STATE(FreeIpaTrustSetupPrepareServerAction.class),
    TRUST_SETUP_CONFIGURE_DNS_STATE(FreeIpaTrustSetupConfigureDnsAction.class),
    TRUST_SETUP_FINISHED_STATE(FreeIpaTrustSetupFinishedAction.class),
    TRUST_SETUP_FAILED_STATE(FreeIpaTrustSetupFailedAction.class),
    FINAL_STATE;

    private Class<? extends FreeIpaTrustSetupBaseAction<?>> action;

    FreeIpaTrustSetupState(Class<? extends FreeIpaTrustSetupBaseAction<?>> action) {
        this.action = action;
    }

    FreeIpaTrustSetupState() {
    }

    @Override
    public Class<? extends FreeIpaTrustSetupBaseAction<?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
