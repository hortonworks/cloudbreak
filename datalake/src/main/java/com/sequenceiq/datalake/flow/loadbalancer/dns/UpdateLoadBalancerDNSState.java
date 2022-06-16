package com.sequenceiq.datalake.flow.loadbalancer.dns;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum UpdateLoadBalancerDNSState implements FlowState {
    INIT_STATE,
    UPDATE_LOAD_BALANCER_DNS_STATE,
    UPDATE_LOAD_BALANCER_DNS_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    UpdateLoadBalancerDNSState() {
    }

    UpdateLoadBalancerDNSState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
