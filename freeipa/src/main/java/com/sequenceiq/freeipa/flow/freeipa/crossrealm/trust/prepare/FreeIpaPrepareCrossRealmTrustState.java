package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action.AbstractPrepareCrossRealmTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action.PrepareCrossRealmTrustConfigureDnsAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action.PrepareCrossRealmTrustFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action.PrepareCrossRealmTrustFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action.PrepareCrossRealmTrustPrepareIpaServerAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action.PrepareCrossRealmTrustValidationAction;

public enum FreeIpaPrepareCrossRealmTrustState implements FlowState {
    INIT_STATE,
    VALIDATION_STATE(PrepareCrossRealmTrustValidationAction.class),
    PREPARE_IPA_SERVER_STATE(PrepareCrossRealmTrustPrepareIpaServerAction.class),
    CONFIGURE_DNS_STATE(PrepareCrossRealmTrustConfigureDnsAction.class),
    PREPARE_CROSS_REALM_TRUST_FINISHED_STATE(PrepareCrossRealmTrustFinishedAction.class),
    PREPARE_CROSS_REALM_TRUST_FAILED_STATE(PrepareCrossRealmTrustFailedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractPrepareCrossRealmTrustAction<?>> action;

    FreeIpaPrepareCrossRealmTrustState(Class<? extends AbstractPrepareCrossRealmTrustAction<?>> action) {
        this.action = action;
    }

    FreeIpaPrepareCrossRealmTrustState() {
    }

    @Override
    public Class<? extends AbstractPrepareCrossRealmTrustAction<?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
