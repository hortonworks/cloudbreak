package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum RefreshEntitlementParamsState implements FlowState {
    INIT_STATE,
    REFRESH_ENTITLEMENT_FAILED_STATE,
    REFRESH_FREEIPA_ENTITLEMENT_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
