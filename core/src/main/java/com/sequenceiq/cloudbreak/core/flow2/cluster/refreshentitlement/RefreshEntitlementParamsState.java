package com.sequenceiq.cloudbreak.core.flow2.cluster.refreshentitlement;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum RefreshEntitlementParamsState implements FlowState {
    INIT_STATE,
    REFRESH_ENTITLEMENT_FAILED_STATE,
    REFRESH_CB_ENTITLEMENT_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
