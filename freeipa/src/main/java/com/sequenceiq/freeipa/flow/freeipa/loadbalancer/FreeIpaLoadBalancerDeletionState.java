package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIpaLoadBalancerDeletionState implements FlowState {
    INIT_STATE,
    LOAD_BALANCER_DELETION_FAILED_STATE,
    LOAD_BALANCER_DNS_DEREGISTRATION_STATE,
    LOAD_BALANCER_CLOUD_DELETION_STATE,
    LOAD_BALANCER_DELETION_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
