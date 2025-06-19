package com.sequenceiq.freeipa.flow.freeipa.trust.setup;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.AbstractTrustSetupAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.TrustSetupConfigureDnsAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.TrustSetupFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.TrustSetupFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.TrustSetupPrepareIpaServerAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.action.TrustSetupValidationAction;

public enum FreeIpaTrustSetupState implements FlowState {
    INIT_STATE,
    VALIDATION_STATE(TrustSetupValidationAction.class),
    PREPARE_IPA_SERVER_STATE(TrustSetupPrepareIpaServerAction.class),
    CONFIGURE_DNS_STATE(TrustSetupConfigureDnsAction.class),
    TRUST_SETUP_FINISHED_STATE(TrustSetupFinishedAction.class),
    TRUST_SETUP_FAILED_STATE(TrustSetupFailedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractTrustSetupAction<?>> action;

    FreeIpaTrustSetupState(Class<? extends AbstractTrustSetupAction<?>> action) {
        this.action = action;
    }

    FreeIpaTrustSetupState() {
    }

    @Override
    public Class<? extends AbstractTrustSetupAction<?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
