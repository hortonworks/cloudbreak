package com.sequenceiq.datalake.flow.loadbalancer.dns;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum UpdateLoadBalancerDNSState implements FlowState {
    INIT_STATE,
    UPDATE_LOAD_BALANCER_DNS_PEM_STATE,
    UPDATE_LOAD_BALANCER_DNS_IPA_STATE,
    UPDATE_LOAD_BALANCER_DNS_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends DefaultRestartAction> restartAction =
            FillInMemoryStateStoreRestartAction.class;

    UpdateLoadBalancerDNSState() {
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
