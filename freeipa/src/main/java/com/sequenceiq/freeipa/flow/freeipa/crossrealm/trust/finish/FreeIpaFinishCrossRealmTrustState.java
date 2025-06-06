package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.action.AbstractFinishCrossRealmTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.action.FinishCrossRealmAddTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.action.FinishCrossRealmTrustFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.action.FinishCrossRealmTrustFinishedAction;

public enum FreeIpaFinishCrossRealmTrustState implements FlowState {
    INIT_STATE,
    ADD_TRUST_STATE(FinishCrossRealmAddTrustAction.class),
    FINISH_CROSS_REALM_TRUST_FINISHED_STATE(FinishCrossRealmTrustFinishedAction.class),
    FINISH_CROSS_REALM_TRUST_FAILED_STATE(FinishCrossRealmTrustFailedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractFinishCrossRealmTrustAction<?>> action;

    FreeIpaFinishCrossRealmTrustState(Class<? extends AbstractFinishCrossRealmTrustAction<?>> action) {
        this.action = action;
    }

    FreeIpaFinishCrossRealmTrustState() {
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
